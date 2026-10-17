# Architecture

The service is a Spring Boot 3.5 application using MyBatis and PostgreSQL 16. The workflow YAML defines states, role-scoped edges, guards and effects. Claim mutations coordinate the claim row, audit records, idempotency receipt, payment request, and outbox in one PostgreSQL transaction.

## Transactions, idempotency, and effects

Each mutation obtains a transaction-scoped advisory lock for its actor/scope/idempotency key, checks the canonical request fingerprint, locks the claim, verifies expected version and workflow conditions, and atomically writes mutation evidence. Matching retries replay the stored receipt; key reuse with another fingerprint conflicts. PostgreSQL constraints, role grants, and append-only audit triggers reinforce application invariants. Payment requests and local outbox rows are transactional; delivery happens after commit with leasing/retry. Changeset 006 adds the persisted W3C trace context to the outbox without modifying prior changesets; six changesets are present.

## API, security, and observability

REST is rooted at /api/v1. Authentication maps only validated principals to domain actors; member visibility is enforced in SQL. CSRF protects browser mutations. Bounded safe correlation IDs are returned and logged separately from trace context. Micrometer traces export through OTLP to the local OpenTelemetry Collector. Inbound W3C parent context is adopted by HTTP server spans; outbox work creates a child span from persisted context, with correlation ID plus trace/span IDs in logs. The local Collector uses a debug exporter for evidence and is not a durable production trace backend. Actuator health is public with hidden details; other actuator routes, including Prometheus, require authentication.

## Diagrams and sources

Each diagram has editable Mermaid source and rendered SVG/PNG output:

- [Claims architecture](images/claims-architecture.png) ([SVG](images/claims-architecture.svg), [source](images/claims-architecture.mmd))
- [Claim workflow](images/claims-workflow.png) ([SVG](images/claims-workflow.svg), [source](images/claims-workflow.mmd))
- [Database schema](images/database-schema.png) ([SVG](images/database-schema.svg), [source](images/database-schema.mmd))
- [Security boundaries](images/security-boundaries.png) ([SVG](images/security-boundaries.svg), [source](images/security-boundaries.mmd))
- [Local deployment](images/deployment.png) ([SVG](images/deployment.svg), [source](images/deployment.mmd))

images/render_diagrams.py regenerates the previews.

## Verification status

Current clean verification passed 123 unit and 9 PostgreSQL/Testcontainers tests, applied six changesets, and measured 97.0464% line and 90.1116% branch coverage. The single combined live acceptance suite passed 29/29, Newman passed 14 assertions, actual exported trace parentage was verified, and all seven measured JMeter profiles passed the user-approved latency/error thresholds. The full load profiles predate a final HTTP log context-only adjustment, as documented. Browser UI and live two-member ownership scenarios remain unverified. The project remains in progress and is not accepted. See [testing evidence](../testing/README.md) and [current verification record](../../reports/verification/REQ-015-final.json).
