# Changelog

## 2026-10-17 — Remaining acceptance evidence (REQ-015/016)

- Completed one combined PostgreSQL-backed live run of 29/29 acceptance checks; saved 31 redacted request/response exchanges and W3C context carriers.
- Added OTLP tracing and an outbox trace-context migration; verified collector-exported HTTP → security → asynchronous outbox parentage and matching application logs.
- Executed seven JMeter profiles against user-approved P95≤500 ms, P99≤1,000 ms, and errors≤1% limits. All selected runs passed; full-run provenance predates a final log-context-only adjustment. Runner JSON metadata/compression was verified by a short post-adjustment run.
- Newman executed 27 requests and 14 assertions with zero failures. Updated all five editable architecture/workflow/database/security/deployment diagram source/render sets.
- Final clean Maven verification passed 123 unit and 9 PostgreSQL/Testcontainers tests, six Liquibase changesets, 97.0464% line and 90.1116% branch coverage. No acceptance status is granted; browser UI, live cross-member ownership, and full bullet-by-bullet live coverage remain open. Raw host-managed LLM payloads remain NOT_CAPTURED/PARTIAL.

## 2026-10-17

- Initialized the approved architecture/implementation plan, Maven/Spring configuration scaffold, initial OpenAPI contract, and prompt/interaction evidence files.
- Re-ran the current T00 contract build with local Java 21/Maven 3.9.11: 8 tests passed, 0 failures/errors/skips; independent rereview is pending for the updated source hash.
- Recorded the no-HEAD/no-verified-main Git state and continued on the existing `develop` branch without resetting or fabricating history.
- Docker socket access remains denied; PostgreSQL, migration startup and runtime checks are not verified.
- Accepted the scoped T01 workflow engine after current-source independent review; the 27-test unit suite passes, while measured overall coverage is below threshold.
- Added T02 typed persistence ports, a non-duplicate changeset 002, role bootstrap/migration/runtime separation, append-only audit guards, and a reviewed-in-progress MyBatis adapter draft. PostgreSQL and application runtime remain unverified because Docker socket access is denied.
- Completed transactional claim use cases, persisted idempotency receipts, claim/system audit, payment/outbox dispatch, Basic-authenticated REST, CSRF-backed browser UI, bounded correlation IDs, Prometheus metrics, security/operations docs, and architecture/workflow diagrams with editable sources.
- Current unit run passes 42 tests (0 failures/errors/skips); measured coverage is 59.76% line / 45.45% branch, below the 95%/90% gates. Independent rereview corrected correlation-ID propagation and found no new source issues in the reviewed paths.
- Attempted four Testcontainers classes; all errored before assertions because Java could not access `/var/run/docker.sock`. Standard package/verify is separately blocked by uncached offline Spring Boot 3.5.16 plugin dependencies. Migrations, startup, end-to-end scenarios and load profiles are unverified; implementation is not accepted or production-ready.
- Final current-source unit run passes 42/42, including 3 proxy/audit aspect regressions; current coverage is 59.76% line / 45.45% branch, below quality gates. Independent rereview closes the proxy advice caveat; PostgreSQL and `REQUIRES_NEW` persistence remain unverified.
- Added a root README with implemented capabilities, local setup, honest verification state, and the unborn-develop Git constraint.
- Resolved Git baseline: local checkout and remote origin have no commit/branch history. A full worktree snapshot was preserved at `refs/codex/snapshots/pre-main-20261017`; the requested main history will start from a planning/contracts-only baseline, while unverified application files remain on develop.
- Recorded the user-authorized planning/contracts-only `main` baseline at `521cb1427f910ebbd01129cc1a950c55b899ee87`; application implementation stays on `develop`.
- Added meaningful PostgreSQL integration coverage for transaction rollback across claim/audits/idempotency/payment/outbox, concurrent same-key receipt locking, and competing same-version CAS updates. The first rollback assertion exposed an incorrect expectation about rollback-only transaction return behavior; corrected it to verify absence of all persisted rows.
- Final clean Maven quality run passed 120 unit tests and 9 PostgreSQL 16/Testcontainers tests, applied all five Liquibase changesets, and measured 892/916 lines (97.3799%) and 549/609 branches (90.1478%). No production class exclusions were added.
- Repeated Compose setup against the existing database volume succeeded; both services were healthy, database rows persisted (2 before and 3 after the new scenario), authenticated smoke passed, and `scripts/run-scenarios.py` passed 20/20 live HTTP checks.
- Independent integrated review found no critical/high issues. It identified a test-only CAS race; both workers now use one pre-captured expected version, and focused rereview confirmed the fix. Advisory lock test retains a 250ms timing-sensitive negative wait.
- Validator restored from authoritative workflow rules; validator unit tests pass 12/12 and the 13-task manifest validates.
- Updated current verification records. The full live 29-case acceptance matrix, JMeter 100/500-client profiles, and trace/span propagation remain `NOT_RUN`; project and task remain in progress and are not accepted.
- Committed the reviewed application, tests, docs and evidence on `develop` as `207c70d7e559af9be86fb5cf9e9b538a42b8bf22` (parent `521cb1427f910ebbd01129cc1a950c55b899ee87`); no push or merge.
