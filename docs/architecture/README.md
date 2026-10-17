# Architecture

The application is one Spring Boot 3.5.16 service with MyBatis and PostgreSQL 16. The YAML workflow is loaded into an immutable definition; named guard/effect strategies resolve configured identifiers. REST maps only authenticated principals to domain actors. Claim use cases own transactions and coordinate the claim row, claim transition audit, supplementary system-operation audit, idempotency receipt, payment request and local outbox.

## Transaction and effects

Every mutation obtains a transaction-scoped advisory lock for `(actor, scope, idempotency key)`, checks its canonical request fingerprint, locks the claim row, validates expected version and workflow guards, and writes the updated claim plus all success evidence in one transaction. A retry with the same fingerprint returns its receipt; key reuse with another fingerprint conflicts. The database also enforces a unique receipt key, version checks, append-only audit triggers, and runtime-role column grants. A rejected operation may append a safe failure record through `REQUIRES_NEW` after its business transaction rolls back.

An approved claim writes a payment request in the mutation transaction. The configured payment trigger creates a local outbox row in that same transaction. A scheduled dispatcher leases due rows, invokes mock delivery after the original commit, confirms a payment reference, and records delivery or a retry time. No external broker is part of this design.

## API and observability

The API is rooted at `/api/v1`. Claim and audit collections use zero-based `page` (default 0), `size` (default 20, maximum 100), deterministic ordering, and typed count/page responses. Member ownership filtering remains in SQL. `X-Correlation-ID` accepts only bounded safe ASCII, otherwise receives a generated UUID; the same effective value is returned, logged, and passed to mutation audit. It is kept separate from Micrometer observation trace/span context. Actuator health is public with hidden details; other actuator routes, including Prometheus, require authentication.

## Diagrams and sources

- [Claims architecture diagram](images/claims-architecture.png) ([SVG](images/claims-architecture.svg), [editable Mermaid source](images/claims-architecture.mmd))
- [Configured claim workflow](images/claims-workflow.png) ([SVG](images/claims-workflow.svg), [editable Mermaid source](images/claims-workflow.mmd))
- PNG previews are reproducibly rendered by [render_diagrams.py](images/render_diagrams.py).

## Verification status

The latest clean quality suite passes 120 unit tests and 9 PostgreSQL 16/Testcontainers integration tests, applies all five Liquibase changesets, and measures 97.3799% line and 90.1478% branch coverage. Repeat Compose startup, authenticated smoke checks, and all 20 live claims API scenarios passed. The integrated review reported no remaining high or critical issue after a compare-and-set test synchronization fix. The application remains in progress and is not marked accepted. A combined live 29-case matrix and JMeter load runs remain unexecuted. See [verification status](../testing/README.md) and [`reports/verification/T08-final.json`](../../reports/verification/T08-final.json).
