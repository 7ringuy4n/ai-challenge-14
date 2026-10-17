# AD-01 scoped re-review — attempt 01

- Document identity and field authorization: **ADDRESSED**. Stable ID grammar, member-only `present` patch for existing IDs, prohibited member roster/required/valid changes, clerk permissions, and lifecycle restrictions are explicit.
- Top-level shallow replacement and keyed document-field merge: **ADDRESSED**.
- Residual finding: **NOT FULLY ADDRESSED**. Clerk roster removal has no defined representation; omission/null behavior is ambiguous because `ClaimFacts` permits null. Reviewer recommends specifying whether omitted IDs are preserved/deleted and how null behaves.
- No other concrete inconsistency found.

Read-only inspection only; no checks run. Reviewer gpt-6-luna / medium selected, actual model NOT_CAPTURED. Host payloads NOT_CAPTURED; trace PARTIAL.
