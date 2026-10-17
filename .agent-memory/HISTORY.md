# Architecture and decision history

## 2026-10-17 — Initial project design

- New single-deployable Java 21/Spring Boot/PostgreSQL claims workflow service; no source implementation was present.
- Use YAML workflow strategies, MyBatis/Liquibase, transactional claim/domain audit/system audit/idempotency/outbox operations, REST/browser UI, and separate correlation and trace IDs.
- The project checkout is unborn; work is on `develop`. Origin exposes no branch refs in this environment.

## 2026-10-17 — T01 scoped acceptance and T02 persistence design

Accepted the scoped, config-driven T01 workflow engine only after independent
review of source hash 70388cbebd58b763b596557d6ccc5a67b507726b4b6edc96cb41ad3161db7570.
It resolves named guard/effect strategies and plans typed effects without I/O.
Overall coverage remains under gates. T02 adds typed persistence ports and a
forward-only schema delta that avoids columns already in changeset 001. Database
roles are provisioned as bootstrap (initialization), migration owner (schema
DDL), and restricted runtime (explicit table/column grants). These database
assumptions require PostgreSQL integration tests and have not been executed
because Docker socket access is denied. A MyBatis adapter patch is pending
independent review; it is not accepted yet.

## 2026-10-17 — Integrated implementation, baseline, and final verification evidence

The user authorized an empty-remote baseline. Before creating branches, the complete pre-change worktree was preserved at `refs/codex/snapshots/pre-main-final-20261017`. The planning/contracts-only root commit `521cb1427f910ebbd01129cc1a950c55b899ee87` is on `main`; all app implementation remains on its direct child `develop`. No remote refs were advertised and no push, merge, force, or reset occurred.

The service implementation includes transactional persistence/idempotency, success/failure audit, authenticated REST/UI, payment/outbox workflow, correlation/metrics, and architecture/operations docs. Meaningful tests increased coverage from 59.76% line / 45.45% branch to 97.3799% line (892/916) / 90.1478% branch (549/609), with no production exclusions. Full clean Maven quality verification passed 120 unit plus 9 PostgreSQL 16/Testcontainers tests, 0 failures/errors/skips, and all five Liquibase changesets. PostgreSQL coverage now includes Spring transaction rollback across claim/audits/receipt/payment/outbox, concurrent same-key receipt locking, CAS contention, runtime grants, and audit immutability.

Direct Docker socket and localhost access are denied in the default sandbox; approved escalated invocations succeeded without changing permissions. Compose setup/rebuild against an existing volume passed twice, services became healthy, prior rows survived, authenticated smoke passed, and `scripts/run-scenarios.py` passed 20/20 live HTTP checks. Workflow validator has 12 passing unit tests and validates 13 tasks. An independent integrated review found no remaining critical/high finding after its P2 CAS-test synchronization finding was fixed and rereviewed. Advisory-lock test's bounded negative wait remains a timing sensitivity.

The application and tasks remain in progress, not accepted or complete. A single live 29-case matrix, JMeter 100/500-client runs, and trace/span propagation remain `NOT_RUN`. Host-managed raw model payloads remain `NOT_CAPTURED`; trace audit remains `PARTIAL`. See `reports/verification/T08-final.json` and `reports/sub-agents/T07/final-integrated-review.md`.

The complete reviewed application and verification package was committed to `develop` as `207c70d7e559af9be86fb5cf9e9b538a42b8bf22`, directly on planning baseline `521cb1427f910ebbd01129cc1a950c55b899ee87`; no push or merge was performed. `reports/verification/git-history.md` records the failed missing-identity attempt and the one-commit local author override.

The verification/Git ledger follow-up was committed as `cd4032b2e25940df261b0dfca301429a54fe87ec` after implementation commit `207c70d7e559af9be86fb5cf9e9b538a42b8bf22`. The task remains in progress with the unified 29-case live suite, JMeter profiles and trace/span propagation still open.

## 2026-10-17 — REQ-017 conditional merge policy

The latest request supersedes REQ-015's no-merge clause by authorizing a local merge only after every acceptance gate passes. It does not authorize a push. Unlike the earlier partial audit posture, full historical host-managed payload capture is now mandatory. If local or supported exports cannot recover those payloads, accurately document irrecoverability and stop before merge pending the user's explicit acceptance-policy decision. Work is tracked under T08 with coordinator-owned T13–T16 evidence paths.


## 2026-10-17 — REQ-017 live verification progress

Implemented the approved local `CLAIMS_MEMBER_USERNAMES` configuration (default `member-demo`), fixed whole-number JSON amount normalization so persisted money has scale two, and corrected UI reason-entry availability. Fresh Compose rebuild succeeded with PostgreSQL, app, and OTEL collector healthy. The combined live API suite passed 29/29; Chrome 155 CDP passed 17/17 with 155 requests and 15 screenshots; two-principal live ownership passed 7/7 with unchanged protected database state; Postman passed 24 requests/14 assertions; smoke passed.

Final-source Java 21/Maven quality verification passed 126 unit and 9 PostgreSQL/Testcontainers tests, 0 failures/errors/skips, six Liquibase changesets, 97.0134% line coverage (942/971) and 90.2022% branch coverage (580/643). OpenTelemetry evidence from the same combined live run verifies inbound parent → HTTP server → secured request → two asynchronous outbox spans and matching application log trace/span IDs. Independent T14 review found a P2 in Python `assert` checks under `-O`; it was fixed and independently rereviewed PASS.

Still in progress: final-source full seven-profile JMeter rerun and local Codex session audit. Host-managed model raw payloads remain `NOT_CAPTURED`/`PARTIAL` and the user's mandatory full-capture merge condition remains unresolved. No merge or push is performed. Full status and evidence: `reports/verification/REQ-017-current.json`.

## REQ-017 follow-up — boundary review and benchmark provenance

All seven clean-source JMeter profiles were independently reviewed and passed on c6dccfe. A subsequent source review identified a P2 member-identity storage-length boundary; the 127-character limit was implemented and independently rereviewed PASS, with 5/5 focused tests and 127+9 full quality tests passing. The prior performance reports remain tied to c6dccfe and are not final-source acceptance evidence; rerun all profiles after committing the correction.

## 2026-10-17 — REQ-017 final-source acceptance blockers

Commit `0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2` includes the reviewed 127-character member identity boundary correction. The current quality gate passed 127 unit + 9 PostgreSQL/Testcontainers tests with 97.0195% line and 90.2326% branch coverage. Seven unchanged JMeter profiles ran on clean source; six pass, stress P95 564ms exceeds the approved 500ms target (P99 767ms, 0 errors). A default repeat also missed at P95 917ms/P99 1077ms. Temporary Hikari-40 and Tomcat-500 variants performed worse; independent review says no code change is justified from sequential runs on an evolving database and calls for controlled same-data test records. Keep documented defaults. Latest accessible local session export: 64 sessions / 8,172 records, all transcript hashes/paths verified. Host-managed raw model payloads remain NOT_CAPTURED and full audit remains mandatory/BLOCKED. No acceptance, main merge, or push.
## 2026-10-17 — REQ-017 reporting evidence closeout

T15 report task accepted by independent rereview 04, but the application project remains NOT_ACCEPTED. Current live evidence links were reconciled; same-run app logs show HTTP/outbox trace correlation and earlier collector span evidence is labeled separately. Latest audit: 65 sessions, 8,483 accessible records, all 65 hashes valid; ledger 101 rows / 60 interaction IDs. Host-managed raw payloads remain NOT_CAPTURED. Performance acceptance remains FAIL at stress P95 564 ms vs 500 ms. No merge/push.
