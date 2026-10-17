#!/usr/bin/env python3
"""Verify a sanitized Codex ZIP retains session structure and has no scanned hits."""

from __future__ import annotations

import argparse
import collections
import hashlib
import json
import zipfile
from pathlib import Path


REPO = Path(__file__).resolve().parents[2]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def records(raw: bytes, archive_name: str) -> list[dict]:
    result = []
    for line_number, line in enumerate(raw.splitlines(), 1):
        try:
            row = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"invalid JSONL at {archive_name}:{line_number}") from exc
        if not isinstance(row, dict):
            raise ValueError(f"non-object JSONL record at {archive_name}:{line_number}")
        result.append(row)
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("sanitized", type=Path)
    parser.add_argument("--sanitization-report", type=Path, required=True)
    parser.add_argument("--sensitivity-report", type=Path, required=True)
    args = parser.parse_args()
    source, sanitized = args.source.resolve(), args.sanitized.resolve()
    report = json.loads(args.sanitization_report.read_text(encoding="utf-8"))
    sensitivity = json.loads(args.sensitivity_report.read_text(encoding="utf-8"))

    if sha256(source) != report.get("source_sha256"):
        raise ValueError("original archive changed since sanitization")
    if sha256(sanitized) != report.get("sanitized_archive_sha256"):
        raise ValueError("sanitized archive hash differs from report")
    if sensitivity.get("archive_sha256") != report.get("sanitized_archive_sha256"):
        raise ValueError("sensitivity report is for a different sanitized archive")
    nonzero = {key: value for key, value in sensitivity.get("pattern_match_counts", {}).items() if value}
    if nonzero:
        raise ValueError(f"active sensitive-pattern matches remain: {sorted(nonzero)}")

    with zipfile.ZipFile(source) as raw_zip, zipfile.ZipFile(sanitized) as clean_zip:
        if raw_zip.testzip() is not None or clean_zip.testzip() is not None:
            raise ValueError("ZIP CRC verification failed")
        if raw_zip.namelist() != clean_zip.namelist():
            raise ValueError("archive member paths or order changed")
        file_count = 0
        record_count = 0
        session_ids: set[str] = set()
        for name in raw_zip.namelist():
            if name.endswith("/"):
                continue
            file_count += 1
            if not name.endswith(".jsonl"):
                raise ValueError(f"unexpected non-JSONL file: {name}")
            original_rows = records(raw_zip.read(name), name)
            sanitized_rows = records(clean_zip.read(name), name)
            if len(original_rows) != len(sanitized_rows):
                raise ValueError(f"record count changed in {name}")
            original_types = collections.Counter(row.get("type", "UNKNOWN") for row in original_rows)
            sanitized_types = collections.Counter(row.get("type", "UNKNOWN") for row in sanitized_rows)
            if original_types != sanitized_types:
                raise ValueError(f"record type histogram changed in {name}")
            original_ids = [
                row.get("payload", {}).get("id")
                for row in original_rows
                if row.get("type") == "session_meta" and isinstance(row.get("payload"), dict)
            ]
            sanitized_ids = [
                row.get("payload", {}).get("id")
                for row in sanitized_rows
                if row.get("type") == "session_meta" and isinstance(row.get("payload"), dict)
            ]
            if original_ids != sanitized_ids:
                raise ValueError(f"session metadata IDs changed in {name}")
            session_ids.update(str(value) for value in sanitized_ids if value)
            record_count += len(original_rows)

    print(f"PASS: {file_count} JSONL files, {record_count} records, {len(session_ids)} session IDs preserved")
    print(f"PASS: all ZIP CRCs and member names match; sensitive-pattern counts are zero")
    print(f"Source SHA-256 unchanged: {report['source_sha256']}")
    print(f"Sanitized SHA-256: {report['sanitized_archive_sha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
