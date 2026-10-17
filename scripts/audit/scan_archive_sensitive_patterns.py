#!/usr/bin/env python3
"""Count possible sensitive patterns in a ZIP without printing matched values."""

from __future__ import annotations

import argparse
import json
import re
import zipfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path


PATTERNS = {
    "private_key_pem": re.compile(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "github_token": re.compile(rb"\bgh[pousr]_[A-Za-z0-9_]{20,}\b"),
    "aws_access_key": re.compile(rb"\bAKIA[0-9A-Z]{16}\b"),
    "bearer_credential": re.compile(rb"(?i)\bBearer\s+[A-Za-z0-9._~+/=-]{20,}"),
    "credential_assignment": re.compile(
        rb'''(?i)(?:password|passwd|api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret)\s*["']?\s*[:=]\s*["']?(?!\[REDACTED_|NOT_CAPTURED)[^\s,"'}]{12,}'''
    ),
    "email_like": re.compile(rb"\b[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,190}\.[A-Za-z]{2,24}\b"),
    "phone_like": re.compile(rb"(?<!\w)(?:\+\d{1,3}[ .-]?)?(?:\(\d{3}\)|\d{3})[ .-]\d{3}[ .-]\d{4}(?!\w)"),
    "claim_reference_like": re.compile(rb"(?i)\b(?:claim|member|policy)[-_ ]?(?:id|number)\s*[:=#]?\s*[A-Z0-9-]{5,}\b"),
}


def scan(archive: Path) -> dict[str, object]:
    counts: Counter[str] = Counter()
    files: Counter[str] = Counter()
    with zipfile.ZipFile(archive) as zipped:
        for info in zipped.infolist():
            if info.is_dir():
                continue
            content = zipped.read(info)
            for name, pattern in PATTERNS.items():
                if name == "credential_assignment":
                    found = sum(
                        1
                        for match in pattern.finditer(content)
                        if b"[REDACTED_" not in match.group(0).upper()
                        and b"NOT_CAPTURED" not in match.group(0).upper()
                    )
                else:
                    found = len(pattern.findall(content))
                counts[name] += found
                if found:
                    files[name] += 1
        member_count = len(zipped.infolist())
        file_count = sum(not item.is_dir() for item in zipped.infolist())
    import hashlib

    return {
        "scanned_at_utc": datetime.now(timezone.utc).isoformat(),
        "archive": str(archive),
        "archive_sha256": hashlib.sha256(archive.read_bytes()).hexdigest(),
        "archive_member_count_including_directories": member_count,
        "archive_file_count": file_count,
        "pattern_match_counts": dict(counts),
        "files_with_pattern_matches": dict(files),
        "matched_values_emitted": False,
        "qualification": "Heuristic screening only; quoted examples and non-secret data may match. A zero result would not prove absence of all sensitive material.",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("--json", dest="output", type=Path, help="write the count-only report to this path")
    args = parser.parse_args()
    report = scan(args.archive)
    rendered = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
