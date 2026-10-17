# Claims Workflow Orchestrator

A Java 21 / Spring Boot service for configuration-driven claim workflows. The application stores claims, transition and system audit records, idempotency receipts, payment requests, and payment outbox events in PostgreSQL through MyBatis and Liquibase.

## Implemented capabilities

- YAML-defined claim states, transitions, guards, roles, and effect plans.
- Transactional claim mutations with version checks, actor/scope idempotency, and receipt replay.
- Append-only claim and system audit records, including separately recorded failure metadata.
- Authenticated REST and browser UI, server-derived actors, authorization and CSRF protections.
- Payment request and local outbox processing with scheduled mock delivery and retry state.
- Correlation IDs, Micrometer metrics, health endpoint, and Prometheus endpoint.

See [API contract](docs/api/openapi.yaml), [architecture](docs/architecture/README.md), [security](docs/security/README.md), and [operations](docs/operations/README.md).

## Local setup

Prerequisites: Java 21, Maven 3.9+, Docker Engine with Compose, and a Docker daemon the current user can access.

```bash
bash scripts/setup.sh
bash scripts/smoke.sh
```

The setup script creates `.env` from `.env.example` when needed, generates placeholder secrets, validates Compose configuration, and starts PostgreSQL and the application. Keep `.env` private. The app is published to loopback on port 8080 by default. See the operations guide for environment variables, roles, endpoints, and shutdown details.

## Current verification

The latest clean quality run passes 120 unit tests and 9 PostgreSQL 16/Testcontainers integration tests (0 failures/errors/skips), applies all five Liquibase changesets, and measures 97.3799% line and 90.1478% branch coverage. A repeat Compose setup left the existing database volume intact; the application started healthy, authenticated smoke checks passed, and the 20-case claims lifecycle runner passed. An independent integrated review found no critical/high findings; its one test reliability finding was fixed and rereviewed. This work remains in progress and is not marked accepted. JMeter 100/500-client runs and the complete 29-case acceptance matrix have not been executed as a single end-to-end suite. See [testing status](docs/testing/README.md) and [`reports/verification/T08-final.json`](reports/verification/T08-final.json).

## Source control state

The initial `main` baseline is `521cb1427f910ebbd01129cc1a950c55b899ee87` and contains project requirements, rules, task plan, and design/API contracts only. `develop` is based directly on that commit. The full implementation worktree is preserved in Git tree `724342aec1c5cb83ea8bbe4b488ac25a2409ebaf` at `refs/codex/snapshots/pre-main-final-20261017`; current app changes are uncommitted on `develop` pending review and grouped commits. Origin currently advertises no branches. No reset, merge, push, or force operation was performed. See [Git history ledger](reports/verification/git-history.md).
