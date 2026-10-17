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
