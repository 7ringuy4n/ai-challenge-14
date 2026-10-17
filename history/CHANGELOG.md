# Technical changelog

## 2026-10-17 — REQ-015/016 acceptance continuation

A clean quality run passed 123 unit plus 9 PostgreSQL 16/Testcontainers tests, all six Liquibase changesets, and JaCoCo at 97.0464% lines (920/948) and 90.1116% branches (565/627). The combined live PostgreSQL-backed acceptance runner passed 29/29 and preserved 31 redacted exchanges. Newman passed 27 requests/14 assertions. Seven JMeter profiles passed the thresholds selected by the user (P95 ≤500 ms, P99 ≤1,000 ms, errors ≤1%); full runs predate a final log-only trace context change, and one 5-second post-change run checked the runner rather than load behavior. Exported Collector evidence verifies HTTP server, security child and asynchronous outbox child spans, correlated to application logs. All required diagram families have editable source and SVG/PNG outputs. Independent review found no critical/high issues; a P2 evidence token issue and a P3 stale test name were fixed. Remaining: browser UI and live two-member ownership checks, complete per-requirement HTTP coverage, byte-identical full JMeter after logging adjustment, host LLM raw payload capture. Project is NOT_ACCEPTED; see reports/verification/REQ-015-final.json.

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

## 2026-10-17 — REQ-017

- Preserved the user's full mandatory completion and conditional merge request verbatim in `docs/submission/user-prompts.jsonl` and registered REQ-20261017-017.
- Current verification work is split into T13 (byte-identical JMeter), T14 (browser and live cross-member), T15 (requirement traceability/final matrix), and T16 (local session/LLM audit), all under T08 and coordinator-only evidence ownership.
- Main merge is conditionally authorized only after all gates pass; full historical host-managed payload capture cannot be waived. No push is authorized.


## 2026-10-17 — REQ-017 live verification progress

Implemented the approved local `CLAIMS_MEMBER_USERNAMES` configuration (default `member-demo`), fixed whole-number JSON amount normalization so persisted money has scale two, and corrected UI reason-entry availability. Fresh Compose rebuild succeeded with PostgreSQL, app, and OTEL collector healthy. The combined live API suite passed 29/29; Chrome 155 CDP passed 17/17 with 155 requests and 15 screenshots; two-principal live ownership passed 7/7 with unchanged protected database state; Postman passed 24 requests/14 assertions; smoke passed.

Final-source Java 21/Maven quality verification passed 126 unit and 9 PostgreSQL/Testcontainers tests, 0 failures/errors/skips, six Liquibase changesets, 97.0134% line coverage (942/971) and 90.2022% branch coverage (580/643). OpenTelemetry evidence from the same combined live run verifies inbound parent → HTTP server → secured request → two asynchronous outbox spans and matching application log trace/span IDs. Independent T14 review found a P2 in Python `assert` checks under `-O`; it was fixed and independently rereviewed PASS.

Still in progress: final-source full seven-profile JMeter rerun and local Codex session audit. Host-managed model raw payloads remain `NOT_CAPTURED`/`PARTIAL` and the user's mandatory full-capture merge condition remains unresolved. No merge or push is performed. Full status and evidence: `reports/verification/REQ-017-current.json`.

## 2026-10-17 — REQ-017 final verification progress

All seven byte-identical JMeter profiles ran against clean source commit `c6dccfef1ba24292f078b826e430c19d8ec74e92` and passed P95 ≤500 ms, P99 ≤1,000 ms, and errors ≤1%; independent evidence review passed. 1,564,951 samples, compressed JTLs, dashboard archives, resource/database/outbox measurements, and comparison are retained under `reports/verification/REQ-017-jmeter-rerun/`. Local session export/integrity verification passed for 62 sessions and 7,287 accessible records. Raw host-managed payloads remain unavailable and block acceptance/merge.

## 2026-10-17 — REQ-017 review remediation

Resolved the independent P2 for oversized configured member usernames by capping total length at 127, matching the 120-character persisted identity column after the `member-` prefix. Boundary review passed; focused test passed 5/5; full Maven quality suite passed 127 unit + 9 PostgreSQL/Testcontainers tests with 97.0195% line / 90.2326% branch coverage. The seven prior JMeter profiles are valid for c6dccfe only; rerun against the new committed source before final acceptance.
