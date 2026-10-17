# Project history

## 2026-10-17 — Fresh project setup and implementation continuation

The workspace is a fresh, untracked project initialization on branch `develop`.
There is no commit at `HEAD`, no `main` or `origin/main` reference, and the
configured remote advertises no branches. Internal Codex snapshot refs are not
treated as verified main history. Per the user's latest instruction, no branch
reset, synthetic history, merge or push is performed; any later operation that
requires a valid main base must first be clarified with the user.

The T00 source contract was updated to include policy-limit/currency domain values
and domain command/result signatures. Local Maven verification passed 8 contract
tests on the latest recorded source hash; the refreshed independent review is in
progress. The service implementation remains incomplete. Docker CLI is installed,
but access to `/var/run/docker.sock` is denied, so database, migration, Compose
runtime and startup checks are not verified. Host-managed LLM payload access is
unavailable; audit status remains `PARTIAL`.

See `.agent-memory/task_on_progress.md`, `docs/challenge-14/REFERENCE-SCOPE.md`,
and `reports/verification/T00-contract-check-current.json`.

## 2026-10-17 — Workflow engine and persistence foundation

The current T01 workflow implementation passed independent review at source hash
`70388cbebd58b763b596557d6ccc5a67b507726b4b6edc96cb41ad3161db7570` and was
accepted for its scoped engine criteria. The full unit suite now passes 27 tests.
JaCoCo measures 81.62% line and 61.92% branch coverage, below project gates.

T02 now has typed repository ports and workflow metadata, a changeset that only
adds columns absent from 001, append-only audit tables/triggers, idempotency,
payment and outbox schemas, and separate bootstrap, migration-owner and runtime
credentials in Compose initialization. The T02/11 mapper draft is still pending
review and integration. Docker socket access is denied, so PostgreSQL migration,
Testcontainers and startup checks remain unverified. The application remains
incomplete and is not production-ready.

## 2026-10-17 — Transactional service, REST and evidence update

Implemented transactional claim use cases for create, facts updates, human and
system transitions, rejection acknowledgement, paged reads and audit history.
Mutations use advisory idempotency locks, canonical request fingerprints,
receipt replay, version checks and row locking. Success writes include claim
and system-operation audit; failure audit commits separately. Payment requests
and local outbox intents share the mutation transaction, with scheduled mock
delivery and retry after commit. Added authenticated REST, CSRF-backed UI,
correlation handling, request-size limits, Prometheus metrics, and architecture,
workflow, operations, security and testing records.

The independent integrated rereview found and helped correct a correlation-ID
consistency issue: API mutation audit now receives the validated/generated ID
that the filter returns and logs. The updated source passes 42 unit tests and
static asset/Compose/shell checks. JaCoCo measures 59.76% line and 45.45% branch,
below the required gates. Four selected Testcontainers classes were attempted;
all failed before business assertions because the process cannot access the
Docker socket. Maven package/verify also cannot resolve two uncached Spring Boot
3.5.16 plugin dependencies in offline mode. Database migrations, startup,
end-to-end acceptance scenarios and capacity runs remain unverified. The task is
not accepted or production-ready. Current evidence is in
`reports/verification/T02-current.json`.


The proxy-based audit aspect regression was independently rereviewed on source hash `900ac547584e85a54115a7abfcd767cba440d631be0e3c6a56a0aa645045ae32`; it closes the advice binding caveat. The subsequent implementation continuation resolved the initial empty-remote baseline by preserving a recoverable worktree snapshot and creating the planning/contracts-only `main` root `521cb1427f910ebbd01129cc1a950c55b899ee87`; all application work stays on its direct child `develop`.

Coverage work added domain, application, security, mapper, error-path and DTO assertions without excluding production classes. PostgreSQL 16/Testcontainers now directly verifies CAS contention, same-key transaction lock serialization, append-only audits, idempotency, payment/outbox persistence, and one Spring-managed rollback spanning claim, claim audit, system audit, receipt, payment, and outbox. Final `mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Pquality clean verify` passed 120 unit and 9 integration tests, 0 failures/errors/skips, all five Liquibase changesets, 97.3799% line coverage (892/916), and 90.1478% branch coverage (549/609).

Docker socket and localhost requests are denied by the default sandbox; no permissions were changed. Through approved escalated invocations, Compose setup ran twice, the existing database retained rows across image rebuild/restart, both services became healthy, smoke checks passed, and a credential-safe 20-case HTTP lifecycle completed through payment confirmation and closure. The workflow manifest validator passes 12 unit tests and validates 13 manifest tasks. The independent integrated review found no remaining critical/high issue after the coordinator fixed and rereviewed a CAS test synchronization defect. The 250ms negative wait in the advisory-lock test remains timing-sensitive.

The project is still in progress and not accepted. One combined live execution of all 29 acceptance cases, JMeter 100/500-client profiles, and distributed trace/span propagation remain `NOT_RUN`. The final report is `reports/verification/T08-final.json`; review evidence is `reports/sub-agents/T07/final-integrated-review.md`. Earlier failed attempts remain preserved as historical evidence.
