#!/usr/bin/env python3
"""Create a redacted, structurally preserved copy of a Codex JSONL archive."""

from __future__ import annotations

import argparse
import copy
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


REPO = Path(__file__).resolve().parents[2]
EXPORTER_PATH = Path(__file__).resolve().with_name("export-codex-sessions.py")
SPEC = importlib.util.spec_from_file_location("codex_session_exporter", EXPORTER_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load established transcript scrubber: {EXPORTER_PATH}")
exporter = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(exporter)

EMAIL = re.compile(r"\b[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,190}\.[A-Za-z]{2,24}\b")
PHONE = re.compile(r"(?<!\w)(?:\+\d{1,3}[ .-]?)?(?:\(\d{3}\)|\d{3})[ .-]\d{3}[ .-]\d{4}(?!\w)")
CLAIM_REFERENCE = re.compile(r"(?i)\b(?:claim|member|policy)[-_ ]?(?:id|number)\s*[:=#]?\s*[A-Z0-9-]{5,}\b")
GENERIC_CREDENTIAL_ASSIGNMENT = re.compile(
    r'''(?i)((?:password|passwd|secret|api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret|cookie)\s*["']?\s*[:=]\s*["']?)([^\s,"'}]+)'''
)
COOKIE_HEADER = re.compile(r"(?i)(\bCookie\s*:\s*)([^\r\n]+)")
SECRET_FIELDS = re.compile(r"(?i)(?:password|passwd|secret|token|api[_-]?key|authorization|cookie|csrf|credential)")
PERSONAL_FIELDS = re.compile(
    r"(?i)(?:e-?mail|phone|mobile|telephone|ssn|social.?security|date.?of.?birth|\bdob\b|"
    r"first.?name|last.?name|full.?name|claimant|member.?name|address|postal.?code|zip.?code|"
    r"medical|diagnosis|injury|treatment|health.?data|account.?number|claim(?:ant)?[_-]?(?:id|number|description|details|notes|incident))"
)
PRIVATE_PAYLOAD_FIELDS = re.compile(r"(?i)(?:encrypted_content|system_prompt|developer_prompt|system_instructions|developer_instructions)")
MAX_UNCOMPRESSED = 250 * 1024 * 1024


def sha256(path: Path) -> str:
    result = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            result.update(block)
    return result.hexdigest()


def safe_member(info: zipfile.ZipInfo) -> None:
    path = PurePosixPath(info.filename)
    if path.is_absolute() or ".." in path.parts or "\\" in info.filename or "\x00" in info.filename:
        raise ValueError(f"unsafe archive path: {info.filename!r}")
    if stat.S_ISLNK(info.external_attr >> 16):
        raise ValueError(f"archive symlink is not allowed: {info.filename!r}")


def redact_text(value: str, secrets: list[str], counts: Counter[str]) -> str:
    for secret in secrets:
        if secret and secret in value:
            occurrences = value.count(secret)
            value = value.replace(secret, "[REDACTED_SECRET]")
            counts["local_environment_secret_value"] += occurrences
    for label, pattern, marker in (
        ("authorization_value", exporter.AUTH_VALUE, r"\1[REDACTED_AUTH]"),
        ("bearer_token", exporter.BEARER, "Bearer [REDACTED_TOKEN]"),
        ("credential_assignment", exporter.SECRET_KEYS, r"\1[REDACTED_SECRET]"),
    ):
        value, number = pattern.subn(marker, value)
        counts[label] += number
    value, number = GENERIC_CREDENTIAL_ASSIGNMENT.subn(r"\1[REDACTED_SECRET]", value)
    counts["generic_credential_assignment"] += number
    value, number = COOKIE_HEADER.subn(r"\1[REDACTED_SECRET]", value)
    counts["cookie_header"] += number
    value, number = EMAIL.subn("[REDACTED_EMAIL]", value)
    counts["email"] += number
    value, number = PHONE.subn("[REDACTED_PHONE]", value)
    counts["phone"] += number
    value, number = CLAIM_REFERENCE.subn("[REDACTED_CLAIM_REFERENCE]", value)
    counts["claim_reference"] += number
    return value


def sanitize(value: Any, secrets: list[str], counts: Counter[str]) -> Any:
    if isinstance(value, str):
        return redact_text(value, secrets, counts)
    if isinstance(value, list):
        return [sanitize(item, secrets, counts) for item in value]
    if isinstance(value, dict):
        result = {}
        for key, item in value.items():
            name = str(key)
            if PRIVATE_PAYLOAD_FIELDS.search(name):
                result[key] = "[OMITTED_PRIVATE_PAYLOAD]"
                counts["private_or_encrypted_payload"] += 1
            elif SECRET_FIELDS.search(name):
                result[key] = "[REDACTED_SECRET]"
                counts["sensitive_secret_field"] += 1
            elif PERSONAL_FIELDS.search(name):
                result[key] = "[REDACTED_PERSONAL_OR_CLAIM_DATA]"
                counts["sensitive_personal_or_claim_field"] += 1
            else:
                result[key] = sanitize(item, secrets, counts)
        return result
    return value


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--report", type=Path, help="write a value-free sanitization report")
    args = parser.parse_args()
    source, destination = args.source.resolve(), args.destination.resolve()
    if source == destination:
        parser.error("source and destination must differ")
    if not source.is_file() or not zipfile.is_zipfile(source):
        parser.error(f"source is not a ZIP file: {source}")
    if destination.exists():
        parser.error(f"refusing to overwrite destination: {destination}")
    if not source.is_relative_to(REPO) or not destination.is_relative_to(REPO):
        parser.error("source and destination must remain inside the repository")

    secret_values = exporter.secret_values()
    counts: Counter[str] = Counter()
    total = 0
    record_count = 0
    member_count = 0
    file_count = 0
    destination.parent.mkdir(parents=True, exist_ok=True)
    try:
        with zipfile.ZipFile(source, "r") as incoming, zipfile.ZipFile(destination, "x", compression=zipfile.ZIP_DEFLATED) as outgoing:
            names: set[str] = set()
            for info in incoming.infolist():
                safe_member(info)
                if info.filename in names:
                    raise ValueError(f"duplicate archive member: {info.filename!r}")
                names.add(info.filename)
                member_count += 1
                total += info.file_size
                if total > MAX_UNCOMPRESSED:
                    raise ValueError("archive exceeds safe uncompressed-size limit")
                if info.is_dir():
                    outgoing.writestr(copy.copy(info), b"")
                    continue
                file_count += 1
                raw = incoming.read(info)
                if not info.filename.endswith(".jsonl"):
                    raise ValueError(f"cannot sanitize non-JSONL member safely: {info.filename!r}")
                lines = []
                for line_number, line in enumerate(raw.splitlines(), start=1):
                    try:
                        record = json.loads(line)
                    except json.JSONDecodeError as exc:
                        raise ValueError(f"malformed JSONL at {info.filename}:{line_number}") from exc
                    record_count += 1
                    cleaned = sanitize(record, secret_values, counts)
                    lines.append(json.dumps(cleaned, ensure_ascii=False, separators=(",", ":")))
                payload = ("\n".join(lines) + ("\n" if raw.endswith(b"\n") else "")).encode("utf-8")
                output_info = copy.copy(info)
                output_info.compress_type = zipfile.ZIP_DEFLATED
                outgoing.writestr(output_info, payload, compress_type=zipfile.ZIP_DEFLATED)
    except Exception:
        destination.unlink(missing_ok=True)
        raise

    report = {
        "sanitized_at_utc": datetime.now(timezone.utc).isoformat(),
        "source_archive": str(source.relative_to(REPO)),
        "source_sha256": sha256(source),
        "sanitized_archive": str(destination.relative_to(REPO)),
        "sanitized_archive_sha256": sha256(destination),
        "archive_member_count": member_count,
        "archive_file_count": file_count,
        "jsonl_record_count": record_count,
        "redaction_counts": dict(sorted(counts.items())),
        "matched_values_emitted": False,
        "encrypted_reasoning_and_private_system_developer_payloads": "OMITTED_WITH_MARKERS",
        "raw_source_modified": False,
    }
    rendered = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        if args.report.exists():
            destination.unlink(missing_ok=True)
            parser.error(f"refusing to overwrite report: {args.report}")
        args.report.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(2)
