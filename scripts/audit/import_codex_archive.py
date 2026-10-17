#!/usr/bin/env python3
"""Safely register sanitized Codex session snapshots from a supplied ZIP.

The ZIP is never extracted. Exact duplicates link to existing transcript
evidence; changed and new sessions are exported to unique snapshot files.
Existing audit records are never overwritten.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import re
import stat
import sys
import zipfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from typing import Any

EXPORTER_PATH = Path(__file__).resolve().with_name("export-codex-sessions.py")
EXPORTER_SPEC = importlib.util.spec_from_file_location("codex_session_exporter", EXPORTER_PATH)
if EXPORTER_SPEC is None or EXPORTER_SPEC.loader is None:
    raise RuntimeError(f"cannot load transcript exporter: {EXPORTER_PATH}")
exporter = importlib.util.module_from_spec(EXPORTER_SPEC)
EXPORTER_SPEC.loader.exec_module(exporter)


REPO = Path(__file__).resolve().parents[2]
DEFAULT_ARCHIVE = REPO / "codex-logs.zip"
INVENTORY = REPO / "reports/audit/REQ-017-session-inventory.json"
IMPORT_REPORT = REPO / "reports/audit/REQ-023-codex-archive-import.json"
SNAPSHOT_DIR = REPO / "reports/audit/archive-imports/REQ-023/transcripts"
MAX_TOTAL_UNCOMPRESSED = 250 * 1024 * 1024


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_records(data: bytes) -> list[dict[str, Any]]:
    result = []
    for line in data.decode("utf-8", errors="replace").splitlines():
        try:
            value = json.loads(line)
        except json.JSONDecodeError:
            continue
        if isinstance(value, dict):
            result.append(value)
    return result


def first_meta(records: list[dict[str, Any]]) -> dict[str, Any]:
    for record in records:
        if record.get("type") == "session_meta" and isinstance(record.get("payload"), dict):
            return record["payload"]
    return {}


def validate_member(info: zipfile.ZipInfo) -> None:
    name = info.filename
    path = PurePosixPath(name)
    if path.is_absolute() or ".." in path.parts or "\\" in name or "\x00" in name:
        raise ValueError(f"unsafe archive path: {name!r}")
    mode = info.external_attr >> 16
    if stat.S_ISLNK(mode):
        raise ValueError(f"symlink archive entry is not allowed: {name!r}")


def analyze(archive: Path) -> tuple[dict[str, Any], dict[str, bytes]]:
    inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    by_id = {row["session_id"]: row for row in inventory.get("sessions", [])}
    secrets = exporter.secret_values()
    entries: list[dict[str, Any]] = []
    changed_data: dict[str, bytes] = {}
    seen_names: set[str] = set()
    seen_ids: set[str] = set()
    total_size = 0
    archive_member_total = 0

    with zipfile.ZipFile(archive) as zf:
        archive_member_total = len(zf.infolist())
        for info in zf.infolist():
            validate_member(info)
            if info.is_dir():
                continue
            if info.filename in seen_names:
                raise ValueError(f"duplicate ZIP member: {info.filename!r}")
            seen_names.add(info.filename)
            total_size += info.file_size
            if total_size > MAX_TOTAL_UNCOMPRESSED:
                raise ValueError("archive exceeds safe uncompressed-size limit")
            if not info.filename.endswith(".jsonl"):
                entries.append({"archive_member": info.filename, "classification": "ignored_non_jsonl"})
                continue

            raw = zf.read(info)
            records = read_records(raw)
            meta = first_meta(records)
            session_id = str(meta.get("id", ""))
            if not session_id:
                raise ValueError(f"missing session ID in {info.filename!r}")
            if session_id in seen_ids:
                raise ValueError(f"multiple JSONL members claim session ID {session_id!r}")
            seen_ids.add(session_id)
            if meta.get("cwd") != exporter.PROJECT:
                raise ValueError(f"session {session_id} does not identify this project cwd")

            raw_hash = digest(raw)
            prior = by_id.get(session_id)
            if prior and prior.get("source_sha256") == raw_hash:
                transcript = prior.get("transcript_path")
                if not transcript:
                    raise ValueError(f"existing session {session_id} has no transcript link")
                transcript_path = REPO / transcript
                if not transcript_path.is_file() or exporter.sha256(transcript_path) != prior.get("transcript_sha256"):
                    raise ValueError(f"existing transcript failed path/hash verification: {transcript}")
                classification = "duplicate_verified"
                output_path = transcript
                transcript_hash = prior.get("transcript_sha256")
                exported_count = prior.get("exported_accessible_message_and_tool_records", "NOT_RECOUNTED")
            else:
                classification = "updated_snapshot" if prior else "new_session"
                safe_id = re.sub(r"[^A-Za-z0-9_.-]", "_", session_id).strip(".") or "session"
                output_path = str((SNAPSHOT_DIR / f"{safe_id}-{raw_hash[:16]}.jsonl").relative_to(REPO))
                transcript_hash = None
                exported_count = sum(
                    1 for record in records
                    if not (record.get("type") == "reasoning" and record.get("encrypted_content"))
                    and exporter.exportable(record.get("payload", {}))
                )
                changed_data[output_path] = raw

            entries.append({
                "archive_member": info.filename,
                "session_id": session_id,
                "started_at": meta.get("timestamp", "NOT_CAPTURED"),
                "source_sha256": raw_hash,
                "source_bytes": len(raw),
                "classification": classification,
                "existing_transcript_or_snapshot_path": output_path,
                "transcript_sha256": transcript_hash,
                "accessible_records": exported_count,
                "source_record_counts": dict(sorted(Counter(r.get("type", "UNKNOWN") for r in records).items())),
                "raw_host_model_request_response_envelopes": "NOT_CAPTURED",
            })

    counts = Counter(e["classification"] for e in entries)
    report = {
        "schema_version": 1,
        "imported_at_utc": datetime.now(timezone.utc).isoformat(),
        "repository": str(REPO),
        "archive_path": str(archive),
        "archive_sha256": exporter.sha256(archive),
        "archive_bytes": archive.stat().st_size,
        "archive_integrity": "PASS",
        "archive_member_count": archive_member_total,
        "archive_file_count": len(seen_names),
        "session_jsonl_count": len(seen_ids),
        "classification_counts": dict(sorted(counts.items())),
        "redaction_policy": "Sanitized with scripts/audit/export-codex-sessions.py scrubber; secrets redacted with established markers; encrypted reasoning excluded.",
        "raw_model_payload_status": "NOT_CAPTURED",
        "audit_status": "PARTIAL_NOT_CAPTURED",
        "entries": entries,
    }
    return report, changed_data


def export_snapshot(raw: bytes, output: Path) -> str:
    records = read_records(raw)
    secrets = exporter.secret_values()
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        raise FileExistsError(f"refusing to overwrite existing transcript: {output}")
    with output.open("x", encoding="utf-8") as stream:
        for record in records:
            if record.get("type") == "reasoning" and record.get("encrypted_content"):
                continue
            payload = record.get("payload", {})
            if not isinstance(payload, dict) or not exporter.exportable(payload):
                continue
            row = {
                "timestamp": record.get("timestamp"),
                "ordinal": record.get("ordinal"),
                "record_type": record.get("type"),
                "payload": exporter.scrub(payload, secrets),
            }
            stream.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
    return exporter.sha256(output)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", nargs="?", type=Path, default=DEFAULT_ARCHIVE)
    actions = parser.add_mutually_exclusive_group()
    actions.add_argument("--check", action="store_true", help="validate and report without writing")
    actions.add_argument("--verify-import", action="store_true", help="verify a prior import without writing")
    args = parser.parse_args()
    archive = args.archive.resolve()
    if not archive.is_file():
        parser.error(f"archive does not exist: {archive}")
    if not zipfile.is_zipfile(archive):
        parser.error("input is not a ZIP archive")

    if args.verify_import:
        report = json.loads(IMPORT_REPORT.read_text(encoding="utf-8"))
        if exporter.sha256(archive) != report.get("archive_sha256"):
            raise ValueError("archive SHA-256 differs from imported evidence")
        for entry in report.get("entries", []):
            path = (REPO / entry["existing_transcript_or_snapshot_path"]).resolve()
            if REPO.resolve() not in path.parents or not path.is_file():
                raise ValueError(f"missing or unsafe transcript path: {path}")
            if exporter.sha256(path) != entry.get("transcript_sha256"):
                raise ValueError(f"transcript SHA-256 mismatch: {path}")
        print(f"Verified {len(report['entries'])} archive transcript paths and SHA-256 values")
        print(f"Archive SHA-256: {report['archive_sha256']}")
        print(f"Classification counts: {json.dumps(report['classification_counts'], sort_keys=True)}")
        return 0

    report, raw_by_path = analyze(archive)
    if args.check:
        print(json.dumps({k: v for k, v in report.items() if k != "entries"}, indent=2))
        for row in report["entries"]:
            print(f"{row.get('classification', 'ignored')}: {row.get('archive_member')}: {row.get('session_id', 'N/A')}")
        return 0

    if IMPORT_REPORT.exists():
        raise FileExistsError(f"refusing to overwrite existing import report: {IMPORT_REPORT}")
    created: list[Path] = []
    try:
        for relative, raw in raw_by_path.items():
            path = REPO / relative
            export_hash = export_snapshot(raw, path)
            created.append(path)
            for row in report["entries"]:
                if row.get("existing_transcript_or_snapshot_path") == relative:
                    row["transcript_sha256"] = export_hash
                    break
        IMPORT_REPORT.parent.mkdir(parents=True, exist_ok=True)
        with IMPORT_REPORT.open("x", encoding="utf-8") as stream:
            json.dump(report, stream, ensure_ascii=False, indent=2)
            stream.write("\n")
        print(f"Archive SHA-256: {report['archive_sha256']}")
        print(f"Session snapshots: {report['session_jsonl_count']}")
        print(f"Classification counts: {json.dumps(report['classification_counts'], sort_keys=True)}")
        print(f"Import report: {IMPORT_REPORT.relative_to(REPO)}")
        return 0
    except Exception:
        for path in reversed(created):
            path.unlink(missing_ok=True)
        raise


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(2)
