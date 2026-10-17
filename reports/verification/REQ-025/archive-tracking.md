# REQ-025 — Sanitized archive tracking

The user selected a sanitized ZIP. The original `codex-logs.zip` is unchanged and must remain untracked. The sanitized copy `codex-logs.sanitized.zip` has SHA-256 `009d9ae63f7d6d7b94a42508a74dbeae47f6d97d9d89e3c9fbc48dbc583e2738` and contains 65 members (64 files, 28716 JSONL records). A second sanitizer generation produced the same SHA-256.

All 64 session file paths, record counts/types, and session IDs are preserved. ZIP integrity and structural checks pass. The configured sensitive-pattern scan reports zero active matches; redaction counts and historical initial scan findings are preserved in the adjacent JSON reports. Host-managed model payloads remain `NOT_CAPTURED` and audit status `PARTIAL_NOT_CAPTURED`.

## Commands

- **PASS** `python3 scripts/audit/sanitize_codex_archive.py codex-logs.zip codex-logs.sanitized-check.zip` (exit 0; 2026-10-17T08:11:24.418032+00:00–2026-10-17T08:11:41.678581+00:00); log `reports/verification/REQ-025/deterministic_second_generation.log`.
- **PASS** `unzip -t codex-logs.sanitized.zip` (exit 0; 2026-10-17T08:11:41.700317+00:00–2026-10-17T08:11:41.953242+00:00); log `reports/verification/REQ-025/sanitized_zip_crc.log`.
- **PASS** `python3 scripts/audit/scan_archive_sensitive_patterns.py codex-logs.sanitized.zip --json reports/verification/REQ-025/sanitized-sensitivity-scan.json` (exit 0; 2026-10-17T08:11:41.953465+00:00–2026-10-17T08:11:48.777947+00:00); log `reports/verification/REQ-025/sanitized_sensitive_scan.log`.
- **PASS** `python3 scripts/audit/verify_sanitized_archive.py codex-logs.zip codex-logs.sanitized.zip --sanitization-report reports/verification/REQ-025/sanitization-report.json --sensitivity-report reports/verification/REQ-025/sanitized-sensitivity-scan.json` (exit 0; 2026-10-17T08:11:48.778118+00:00–2026-10-17T08:11:50.310370+00:00); log `reports/verification/REQ-025/sanitized_archive_verification.log`.
- **PASS** `python3 scripts/workflow/verify_workflow.py` (exit 0; 2026-10-17T08:11:50.314736+00:00–2026-10-17T08:11:50.342041+00:00); log `reports/verification/REQ-025/workflow_validation.log`.
- **PASS** `git diff --check` (exit 0; 2026-10-17T08:11:50.342212+00:00–2026-10-17T08:11:50.348893+00:00); log `reports/verification/REQ-025/diff_check.log`.

No raw ZIP was staged. The local branch refs at the start of REQ-025 were `2cafe2507708c6b773865a04b342ea623248bf02`. Sanitized archive and related records were committed on `develop` in `6703222`; evidence commits `5371cd79da337a472937a73be5be9eb6c3c7f5e4` and `aa9c31e724b5abc18a9cf0297fbd0cc249fcad14` record the local fast-forward and final verification. At that verification checkpoint, both local `develop` and `main` pointed to `aa9c31e724b5abc18a9cf0297fbd0cc249fcad14`, with the original commit as an ancestor. `codex-logs.zip` remains untracked with its original SHA-256. No push occurred. T20 remains `in_review` pending independent review.
