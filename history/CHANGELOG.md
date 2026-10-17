# Technical changelog

## 2026-10-17

- Initialized the fresh-project design, Maven/Spring/OpenAPI contracts and raw prompt/interaction evidence.
- Refreshed T00: current Maven compile/test passed 8 tests; independent review is pending against the current source hash.
- Recorded the user's `develop` instruction and unborn Git state in `docs/challenge-14/REFERENCE-SCOPE.md`; no history was fabricated.
- Docker socket access remains denied, so PostgreSQL, migration, Compose and runtime checks remain unavailable.
- Accepted scoped T01 after fresh independent source review; current unit suite passes 27/27 and coverage measures 81.62% line / 61.92% branch.
- Started T02 persistence: typed ports and metadata, changeset 002 without duplicate initial-schema columns, schema-owner/runtime role design, immutable audit triggers and static Compose validation. MyBatis adapters are under review; DB-backed verification is blocked by Docker socket permission denial.
- Implemented service transaction composition and browser/API surface: actor-scoped idempotency, CAS writes, domain/system audit, role-bound workflow options, owner rejection acknowledgement, REST/security/CSRF, local outbox mock dispatch, correlation IDs, Micrometer/Prometheus, and architecture/security/operations documentation.
- Unit verification is 42/42 passing. JaCoCo is 59.76% line / 45.45% branch, below gate. Testcontainers reached Docker discovery but all four selected classes errored before assertions due socket permission denial. Offline package/verify is separately blocked by uncached Boot 3.5.16 plugin dependencies. Current source rereview fixed the effective correlation-ID/audit mismatch.
- Implementation remains in progress. Database migration, startup, HTTP integration and acceptance scenarios, load profiles and production readiness are unverified. Checkout remains unborn `develop`, without a verified main base; no history mutation was performed.
- Updated final evidence to current source hash `900ac547584e85a54115a7abfcd767cba440d631be0e3c6a56a0aa645045ae32`: Maven unit tests pass 42/42, focused audit proxy tests pass 3/3, and JaCoCo measures 59.76% line / 45.45% branch (below gates). Review-17 closes the proxy binding caveat, while DB persistence, `REQUIRES_NEW`, startup and end-to-end acceptance remain unverified. Git remains unborn `develop`; no main-based operation performed.
- Added the root README and synchronized final submission docs; the missing workflow validator is recorded as unavailable with its exact exit code.
- User-authorized Git baseline resolution: preserved the original worktree snapshot, initialized `main` with only requirements/design/contracts at `521cb1427f910ebbd01129cc1a950c55b899ee87`, and continued all implementation on direct-child `develop`; origin remains empty and no remote write was made.
- Added PostgreSQL Testcontainers cases for a Spring-managed rollback spanning claim/audits/receipt/payment/outbox, same-key advisory lock serialization, and same-version CAS contention. Full clean verification passes 120 unit + 9 integration tests, zero failures/errors/skips, all five Liquibase changesets, 97.3799% line and 90.1478% branch coverage.
- Repeated Compose setup against the existing volume, observed healthy app/database and retained claim rows, passed authenticated API smoke, and passed 20/20 live HTTP claim lifecycle checks. Workflow validator passes 12 unit tests and validates all 13 tasks.
- Independent integrated review identified a CAS test race; fixed both contenders to use one captured version, and focused rereview confirms closure. No critical/high findings remain; the 250ms advisory-lock negative wait is documented as timing-sensitive.
- Final verification and limitation reports updated. The full live 29-case matrix, JMeter 100/500-client runs, and trace/span propagation remain NOT_RUN. Project remains in progress and is not accepted.
- Reviewed implementation/evidence commit on develop: `207c70d7e559af9be86fb5cf9e9b538a42b8bf22`, parent planning baseline `521cb1427f910ebbd01129cc1a950c55b899ee87`. No remote write or main merge occurred. Follow-up documentation records this commit and the remaining verification limits.
