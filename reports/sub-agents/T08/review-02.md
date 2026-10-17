# Independent rereview 02 — trace-context logs and exchange redaction

Reviewer: `/root/final_integrated_review` (selected gpt-6-luna, medium). Host-managed raw payload is NOT_CAPTURED; audit status PARTIAL. Review was read-only; no tests were run by the reviewer.

**Verdict:** No new high- or critical-severity finding. Valid `traceparent` values are logged as trace and parent-span IDs, malformed values are ignored, and the saved acceptance record redacts the CSRF token.

- **P3 (fixed):** Test name `stopsObservation` no longer described behavior; it was renamed to `removesMdcWhenDownstreamFails`.
- **Trace claim scope:** parsing/logging inbound IDs alone does not establish active server-span adoption. This finding is resolved by separate actual Collector evidence: the HTTP server span parent matches the supplied header; the security span is its child; the outbox delivery span is a child of security span; and application log IDs match exported span IDs. See `reports/telemetry/trace-verification.json`.
- **Evidence redaction:** saved exchange evidence contains `[REDACTED]` for CSRF token and authorization/cookie values and includes declarations.
