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

## 2026-10-17 — REQ-017 mandatory completion gates

- Added mandatory final-source seven-profile JMeter reruns, real browser E2E, live two-member isolation, per-requirement verification, and a complete locally accessible session audit.
- Conditional local merge to `main` is authorized only after every gate passes. Full host-managed payload capture is an explicit gate; an irrecoverable gap blocks merge and requires the user to decide acceptance policy. Push remains unauthorized.
- Recorded new T13–T16 execution workstreams with coordinator-owned exclusive evidence paths.


## 2026-10-17 — REQ-017 live verification progress

Implemented the approved local `CLAIMS_MEMBER_USERNAMES` configuration (default `member-demo`), fixed whole-number JSON amount normalization so persisted money has scale two, and corrected UI reason-entry availability. Fresh Compose rebuild succeeded with PostgreSQL, app, and OTEL collector healthy. The combined live API suite passed 29/29; Chrome 155 CDP passed 17/17 with 155 requests and 15 screenshots; two-principal live ownership passed 7/7 with unchanged protected database state; Postman passed 24 requests/14 assertions; smoke passed.

Final-source Java 21/Maven quality verification passed 126 unit and 9 PostgreSQL/Testcontainers tests, 0 failures/errors/skips, six Liquibase changesets, 97.0134% line coverage (942/971) and 90.2022% branch coverage (580/643). OpenTelemetry evidence from the same combined live run verifies inbound parent → HTTP server → secured request → two asynchronous outbox spans and matching application log trace/span IDs. Independent T14 review found a P2 in Python `assert` checks under `-O`; it was fixed and independently rereviewed PASS.

Still in progress: final-source full seven-profile JMeter rerun and local Codex session audit. Host-managed model raw payloads remain `NOT_CAPTURED`/`PARTIAL` and the user's mandatory full-capture merge condition remains unresolved. No merge or push is performed. Full status and evidence: `reports/verification/REQ-017-current.json`.

## 2026-10-17 — REQ-017 acceptance evidence update

- Completed the seven clean-source JMeter profiles against commit `c6dccfef1ba24292f078b826e430c19d8ec74e92`; all passed approved latency/error thresholds and independent review.
- Exported and integrity-checked 62 locally accessible project sessions (7,287 accessible records). Raw host-managed payloads remain unavailable; project acceptance and main merge remain blocked.

## 2026-10-17 — Member identity storage boundary

- Fixed independent-review P2: configured member usernames now have a 127-character maximum, ensuring the prefix-stripped identity fits the PostgreSQL `VARCHAR(120)` member ID.
- Added a 127/128-character boundary regression; focused test and the full 127-unit + 9-integration Maven quality suite passed (97.0195% line, 90.2326% branch).
- This source change supersedes the prior benchmark revision; all seven JMeter profiles must rerun against the new commit before performance can be marked final-source PASS.

## 2026-10-17 — Final-source performance and audit reconciliation (REQ-017)

- Committed the independently reviewed member identity boundary correction and functional evidence on develop at 0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2.
- Executed all seven exact JMeter profiles on that source commit. Six met thresholds; stress P95 was 564 ms against the approved 500 ms limit (P99 767 ms, errors 0%). Plan hashes match before/after.
- Repeated default stress and tested temporary pool/thread variants. Results varied on the shared local host; candidates were worse and reviewers advise against tuning. No source tuning or threshold change was adopted. Stress gate is FAIL pending controlled dedicated performance environment.
- Re-exported 64 accessible Codex sessions and 8,172 message/tool records; transcript SHA/path integrity passes. Host-managed raw payloads remain NOT_CAPTURED and block acceptance.
- Overall status remains NOT_ACCEPTED; no merge or push.
## 2026-10-17 — REQ-017 verification reporting

- Corrected current combined live-run console/exchange/trace evidence links; retained same-run application logs showing HTTP/outbox trace-ID correlation and clearly separated earlier collector-span evidence.
- Corrected JMeter aggregate (1,089,197) and Newman request count (34); documented T15 report acceptance after independent rereview 04.
- Refreshed accessible session audit to 65 sessions and 8,483 records with all transcript hashes verified; ledger now has 101 rows and 60 IDs. Host-managed raw payloads remain NOT_CAPTURED.
- Project remains NOT_ACCEPTED: stress P95 misses the approved limit, and full host-managed LLM capture remains blocked. No merge or push.


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
- 2026-10-17: Registered REQ-021 to synchronize accepted local `develop`/`main` history and verify the GitHub repository; remote verification is currently blocked by unavailable network/ref evidence.
- 2026-10-17: Fast-forwarded local `develop` to `main` at `ea89eb3340b0b900f106934aee2a92203c9439e7`; workflow validation passed. GitHub update/verification remains blocked (remote unavailable and push rejected by automatic review); no push succeeded.
- 2026-10-17: Authorized atomic non-force push created GitHub `main` and `develop`; post-push fetch and `ls-remote` verified both at `2cafe2507708c6b773865a04b342ea623248bf02`.

## 2026-10-17 — REQ-023 Codex archive supplement

- Added a non-overwriting archive importer and verified the supplied ZIP (SHA-256 `8e3e6f75cc7351fb071dd95f513d233c2d94833a4fab7ddd9c14321fafc67664`). It links 61 exact duplicates, stores two changed session snapshots separately, and records one new session. All 64 transcript hashes verify; raw host model envelopes remain NOT_CAPTURED/PARTIAL.

## 2026-10-17 — REQ-024 archive tracking authorization

- Owner explicitly requested tracking `codex-logs.zip` and locally merging the associated archive evidence. A value-free sensitivity scan found credential-like and email-like patterns; the unchanged raw ZIP remains unstaged pending owner disposition under the project redaction rule. The new commit must remain local unless separately authorized for push.

- REQ-025: owner selected a sanitized archive. The deterministic redacted ZIP preserves 28,716 records and 64 sessions, passes the configured zero-match scan and structural verifier, and leaves the original raw ZIP unchanged/untracked. Local Git integration and independent review remain pending.

- Sanitized archive and related REQ-023–025 audit evidence committed on `develop` as `6703222`; evidence commit `5371cd79da337a472937a73be5be9eb6c3c7f5e4` was fast-forwarded to local `main`. Both local refs match. Original ZIP remains untracked; no push. T20 remains in review pending independent review.
