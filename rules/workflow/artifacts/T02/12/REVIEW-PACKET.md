# T02/12 independent test-source review

Review candidate `rules/workflow/artifacts/T02/12/candidate.patch` against its handoff, attempt packet, current migrations and production adapters. The patch SHA-256 is `da936984d27d72008ac7d7258a72bb8cc78c71cdc1891acc990c266d3bc422be`; current production mapper adapters exist in the workspace. Review independently whether the tests use the actual mapper/adapters or merely reimplement SQL, whether role provisioning/migration and privilege assertions are sound, and whether the assertions cover audit mutation denial, idempotency uniqueness/fingerprint persistence, payment confirmation, outbox retry/lease/delivery and immutable CAS fields.

Return PASS/REWORK/FAIL with file/line actionable findings and criteria coverage. Identify runtime tests that are NOT_RUN; do not claim T02 or whole-app acceptance. No edits except a report at `reports/sub-agents/T02/re-review-12.md`. No builds/tests, Docker/database actions, or Git mutations.
