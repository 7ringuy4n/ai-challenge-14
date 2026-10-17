# AD-01 scoped re-review — attempt 02

- Finding 1 (stable document IDs and field/actor authorization): **ADDRESSED**.
- Finding 2 (keyed merge, omission, null and deletion semantics): **ADDRESSED**.
- Closure verdict: **FAIL pending clarification**. The document says requiredness/validity can only be maintained before leaving `SUBMITTED`, then separately permits validity changes in `PENDING_INFO`. Clarify lifecycle permissions.
- No other issue found.

Read-only inspection only; no checks run. Reviewer gpt-6-luna / medium selected, actual model NOT_CAPTURED. Host payloads NOT_CAPTURED; trace PARTIAL.
