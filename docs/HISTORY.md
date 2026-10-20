# Project history

## 2026-10-17 — Remaining acceptance verification

REQ-015/016 advanced from implementation with missing integration/performance/observability evidence to actual current evidence. One combined live API run against the running PostgreSQL-backed service passed 29/29 checks; 31 request/response exchanges are retained with Authorization, Cookie, and CSRF values redacted. The final clean Maven -Pquality clean verify passed 123 unit tests and 9 PostgreSQL/Testcontainers cases, ran six Liquibase changesets, and measured 97.0464% line and 90.1116% branch coverage. The user approved P95≤500 ms, P99≤1 s, and HTTP errors≤1%; all seven JMeter profile runs met them. These runs predate a final request log context-field adjustment, so the performance record states that limitation; a post-adjustment five-second run only checked JMeter runner packaging. Newman passed 14 assertions, and actual Collector spans verify incoming HTTP parent propagation through the security and asynchronous outbox child spans with matching app logs. Mermaid and rendered PNG/SVG exist for all five requested diagram families. Independent review found no critical/high findings and the cited P2/P3 items were fixed. Browser UI, live cross-member ownership, requirement-level coverage beyond grouped cases, and host-managed raw LLM payload capture remain open. Status stays in progress and not accepted.

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

The reviewed application and evidence were committed on `develop` as
`207c70d7e559af9be86fb5cf9e9b538a42b8bf22`, directly after the planning baseline
`521cb1427f910ebbd01129cc1a950c55b899ee87`. The working tree was clean after
that commit; a follow-up documentation-only commit records its Git ledger and
final requirement/task synchronization. No push or merge was performed.

## 2026-10-17 — Conditional final merge and evidence gates

REQ-017 requires seven JMeter profiles to rerun against a clean, exact source commit and unchanged plan hashes; real-browser positive/negative UI evidence; live PostgreSQL-backed cross-member isolation; a row for every active requirement; and an inventory/transcript/checksum audit of all accessible project-related Codex sessions and archives. It explicitly makes full historical host-managed payload evidence a mandatory condition. The latest user authorizes a local `develop` to `main` merge only after every gate passes; no push is authorized. If the required historical capture is irrecoverable, do not merge and ask for an acceptance-policy decision. T08 remains running; T13–T16 carry disjoint evidence locations and coordinator-only ownership.


## 2026-10-17 — REQ-017 live verification progress

Implemented the approved local `CLAIMS_MEMBER_USERNAMES` configuration (default `member-demo`), fixed whole-number JSON amount normalization so persisted money has scale two, and corrected UI reason-entry availability. Fresh Compose rebuild succeeded with PostgreSQL, app, and OTEL collector healthy. The combined live API suite passed 29/29; Chrome 155 CDP passed 17/17 with 155 requests and 15 screenshots; two-principal live ownership passed 7/7 with unchanged protected database state; Postman passed 24 requests/14 assertions; smoke passed.

Final-source Java 21/Maven quality verification passed 126 unit and 9 PostgreSQL/Testcontainers tests, 0 failures/errors/skips, six Liquibase changesets, 97.0134% line coverage (942/971) and 90.2022% branch coverage (580/643). OpenTelemetry evidence from the same combined live run verifies inbound parent → HTTP server → secured request → two asynchronous outbox spans and matching application log trace/span IDs. Independent T14 review found a P2 in Python `assert` checks under `-O`; it was fixed and independently rereviewed PASS.

Still in progress: final-source full seven-profile JMeter rerun and local Codex session audit. Host-managed model raw payloads remain `NOT_CAPTURED`/`PARTIAL` and the user's mandatory full-capture merge condition remains unresolved. No merge or push is performed. Full status and evidence: `reports/verification/REQ-017-current.json`.

## 2026-10-17 — REQ-017 performance and audit evidence

The clean-source seven-profile JMeter rerun passed the user-approved P95/P99/error thresholds, was independently reviewed, and is tied to commit `c6dccfef1ba24292f078b826e430c19d8ec74e92`. Local Codex session export covers 62 project-related session files and 7,287 accessible message/tool records with integrity checks. Full host-managed model request/response capture remains `NOT_CAPTURED`, so the explicit acceptance gate is blocked and no main merge is permitted.

## 2026-10-17 — Member username database-length guard

Independent review found configured member usernames could exceed the persisted member ID column after prefix removal. The coordinator added a 127-character username cap and boundary coverage. The focused test passes 5/5; the full suite passes 127 unit + 9 PostgreSQL/Testcontainers tests with 97.0195% line and 90.2326% branch coverage. Prior JMeter evidence remains tied to c6dccfe; final-source rerun is pending for the corrected revision.

## 2026-10-17 — Final-source performance outcome and audit refresh

Source commit 0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2 received the reviewed 127-character local member identity limit. The full quality run passed 127 unit and 9 PostgreSQL/Testcontainers tests with 97.0195% line and 90.2326% branch coverage; combined live, browser, cross-member, Postman and startup evidence passed. All seven exact JMeter profiles ran against the commit and unchanged plan hashes: six pass, stress P95 564 ms exceeds the approved 500 ms limit (P99 767 ms, errors zero). A default repeat also failed. Pool-40 and Tomcat-500 diagnostic runs were worse; an independent reviewer found no justified tuning and cautioned the sequential, changing database does not prove causality. The performance gate is failed pending a controlled dedicated environment. Local audit refresh covers 64 sessions and 8,172 records with transcript hashes/paths verified. Host-managed raw model payloads remain NOT_CAPTURED, blocking acceptance and merge. No push.
## 2026-10-17 — REQ-017 reporting evidence closeout

T15's report package passed independent rereview 04 and is accepted as a reporting task only. The current combined live run retains matching 29/29 console, HTTP exchanges, trace context, and application log evidence. The current app logs correlate HTTP approval and two asynchronous outbox deliveries on trace `d5fdcf613e4a47189047a2bcd20df302`; collector-exported parent-child spans are explicitly evidence from a separate earlier run. JMeter total is 1,089,197 with six profiles passing and stress P95 failing 564 ms versus 500 ms. Latest audit export covers 65 sessions/8,483 accessible records; ledger has 101 rows/60 IDs and host-managed raw payloads remain NOT_CAPTURED. Overall project acceptance remains NOT_ACCEPTED; no merge or push.


## 2026-10-17 — REQ-020 revised acceptance evidence (review pending)

The owner revised local performance acceptance to permit a `HARDWARE_CONSTRAINED` exception when measurements attribute a laptop stress miss to shared-host limits, without changing the original result or implying production-scale performance. The original final-source 500-VU run remains FAIL at P95 564 ms; the monitored repeat is also FAIL at P95 638 ms. Host CPU reached 99.84% and load average 28.47 on eight logical CPUs while JMeter, app and PostgreSQL competed. RAM, swap, thermal counters, GC, DB locking, Hikari timeouts and outbox were also checked; Hikari pending borrowers peaked at 178. The evidence supports a local exception, pending independent review; production performance remains UNVERIFIED.

The owner revised LLM acceptance to proof-of-work. Latest accessible export preserves 65 session transcripts and 8,758 accessible records; transcript hashes, IDs and contained paths pass. The interaction ledger has 101 rows/60 interaction IDs, with explicit requirement/task links on 59 rows. One active coordinator source file continued growing after export and is disclosed. Raw host payloads remain NOT_CAPTURED/PARTIAL.

A fresh Docker-based Maven quality regression passed 127 unit + 9 PostgreSQL/Testcontainers integration tests, six Liquibase changesets, 97.0195% line / 90.2326% branch coverage; workflow validation passed 18 tasks. Revised `FINAL-ACCEPTANCE.md` is pending independent acceptance review. No acceptance, commit or merge yet; no push.


Initial REQ-020 independent review confirmed the measured hardware exception and proof-of-work evidence, but returned REWORK because T13/T16 still showed `running`. The finding is preserved verbatim at `reports/sub-agents/T17/review-01.md`; both task states are now reconciled as accepted under the revised policy, with historical P95 FAIL and raw-payload PARTIAL retained. Focused rereview is required before acceptance.


Focused T17 rereview 02 identified that T13 still said all approved latency limits must pass despite the owner-approved exception. T13's acceptance criteria now preserve the original numerical stress FAIL and limit acceptance to a reviewed local `HARDWARE_CONSTRAINED` exception under REQ-020; production scale remains UNVERIFIED. Review-02 is preserved verbatim at `reports/sub-agents/T17/review-02.md`. Final focused confirmation is pending.


REQ-020 acceptance decision — independent review-03 PASS. T13 now preserves the original P95 564ms FAIL and instrumented 638ms FAIL while accepting only the local HARDWARE_CONSTRAINED exception; production-scale performance is UNVERIFIED. T16 proof-of-work passes for 67 accessible sessions and 8923 records, with raw payload completeness PARTIAL/NOT_CAPTURED. Final current Maven quality regression passed 127 unit + 9 PostgreSQL integration tests and coverage thresholds; workflow validator passes. Marked ACCEPTED_WITH_PERFORMANCE_EXCEPTION. Local merge into existing main is authorized after commit; no push.


REQ-020 final merge: after independent review-03 PASS, T13 accepted the local HARDWARE_CONSTRAINED exception without claiming either stress P95 passed; T16 accepted proof-of-work while raw host payload completeness remains PARTIAL/NOT_CAPTURED. The project is `ACCEPTED_WITH_PERFORMANCE_EXCEPTION`. Accepted develop head `730ca6fd6569a451a286b189f9dd69893d26c197` merged into existing main baseline `521cb1427f910ebbd01129cc1a950c55b899ee87` as `18f97aa9c8296fdc107a02192aa4e8e62dd45d78`. No push. Latest audit export: 67 sessions / 9002 accessible records; ledger 106 rows / 65 interaction IDs.


Post-merge audit refresh: 67 project-related sessions and 9020 accessible records exported; all stored transcript hashes/IDs/paths validate. The actively growing coordinator source log remains a documented point-in-time hash exception. Interaction ledger has 106 rows / 65 IDs, 64 with explicit requirement/task links; raw host envelopes remain NOT_CAPTURED/PARTIAL.


Final Git closeout: accepted develop commit 730ca6fd6569a451a286b189f9dd69893d26c197 is in main through merge 18f97aa9c8296fdc107a02192aa4e8e62dd45d78; post-merge evidence commit d0fd9dd recorded the merged reports and audit. No push.

## 2026-10-17 — REQ-021 branch synchronization request

The owner requested synchronization of `develop` and `main` and verification of the GitHub repository. Local `develop` is an ancestor of accepted `main`; remote verification has not succeeded because DNS resolution failed, web fetch returned a cache miss, and the in-app browser timed out. The remote has not been modified.

REQ-021 local outcome: `develop` was fast-forwarded to `main`; both point at `ea89eb3340b0b900f106934aee2a92203c9439e7`. The workflow validator passes. GitHub refs could not be verified (DNS error / no-ref responses / browser timeout); the non-force SSH push hung and was interrupted, and automatic approval review rejected the HTTPS push before execution because remote ancestry was unverified and explicit push authorization was not established. No push succeeded.

## 2026-10-17 — REQ-022 GitHub synchronization

After explicit owner authorization, an atomic, non-force push created the previously absent `origin/main` and `origin/develop`. A post-push fetch and `git ls-remote` independently verified both refs at `2cafe2507708c6b773865a04b342ea623248bf02`. The push did not include the untracked `codex-logs.zip`. No force push, reset, deletion, or protection bypass occurred. The follow-up evidence/report changes remain uncommitted to preserve both heads at the exact requested commit.

## 2026-10-17 — Supplemental Codex audit archive

REQ-023 adds a safe archive importer that validates ZIP member paths, refuses duplicate session IDs and existing output collisions, links exact source duplicates to existing sanitized transcripts, and creates separate sanitized transcript snapshots for changed/new sessions. Archive hash and 64 transcript hashes are recorded in `reports/audit/REQ-023-codex-archive-import.json`. The historic 67-session inventory is not overwritten; the supplemental archive contains one additional distinct session. Host-managed raw payloads stay NOT_CAPTURED/PARTIAL.

## 2026-10-17 — Archive tracking scope

REQ-024 authorizes committing the supplied Codex archive and directly related audit evidence to `develop`, then merging locally into `main`; it does not authorize a push.

## 2026-10-17 — Sanitized archive decision

The owner selected a sanitized ZIP under REQ-025. The original archive remains byte-for-byte unchanged and untracked. Sanitized output has stable generation, zero active configured sensitive-pattern matches, and preserves all 64 sessions/28,716 records and their type histograms.

The sanitized archive and directly related import/audit evidence were committed on `develop` as `6703222` after reviewing the staged file set. Final verification commit `aa9c31e724b5abc18a9cf0297fbd0cc249fcad14` is the matching tip of local `develop` and `main`. The raw ZIP remained untracked. No push occurred; T20 remains in review pending independent review.

REQ-026: Reconfirmed both local branch archive blobs match sanitized SHA-256 009d9ae63f7d6d7b94a42508a74dbeae47f6d97d9d89e3c9fbc48dbc583e2738; structural verification preserves 64 sessions/28,716 records, all 64 imported transcript hashes verify, and workflow validator passes 21 tasks. Raw ZIP remains unchanged/untracked; no push. T20 remains in_review. Evidence: reports/verification/REQ-026/archive-reconfirmation.json.

## 2026-10-20 — Logical-question source import

REQ-20261020-001 adds the provided Questions.md under `logical-question/`. The copied file matches the supplied source SHA-256 `8bc3049e92a4f15886e0c77e82de8bfbbc9cbe13bb1d97a34329b972d8588dce`. No application architecture or runtime behavior changed.

## 2026-10-20 — REQ-20261020-001 integration

The imported Questions.md remains byte-identical to its supplied source. Independent review passed, and local main was fast-forwarded from the verified origin/main base to develop commit 5230724495d3f1d43bde329ace84ca1f080ea5a9. No remote refs were modified. The unfiltered whitespace warning comes from the source document and is intentionally preserved.

## 2026-10-20 — REQ-20261020-002

The owner authorized a normal push and verification of both GitHub branches. Local `develop` was fast-forwarded to the reviewed local `main` commit `9f0fa9ec689dbfcdf219475c5327fbef5c4ba3dc`; remote branches were observed at their shared base before publication. Final remote results will be recorded in the verification artifact.


REQ-20261020-002: the authorized atomic non-force push succeeded. A fresh fetch verified local and remote `develop`/`main` at `c99ddec1ab0df258b8ecfe2a2deb649d1746134d`; the original remote base was retained as an ancestor. A documentation-only closeout is being published afterward.

T22 independent review 01 identified an allowed-path omission; the coordinator corrected it, and focused rereview 02 passed. Coordinator acceptance is recorded. The documentation-only final evidence closeout is being synchronized to both branches.


REQ-20261020-002 accepted after independent rereview and coordinator approval. GitHub `develop` and `main` were both fetched and verified at `c99ddec1ab0df258b8ecfe2a2deb649d1746134d`. A follow-up documentation commit publishes the acceptance record.

Final closeout commit `0e0e599f2d2a8f76b37c8bf7503d83cb5ece3d07` was fetched and verified as both `origin/develop` and `origin/main`; original base ancestry is preserved.
