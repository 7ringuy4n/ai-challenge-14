# Operations

## Local runtime

Use Java 21, Maven 3.9.11, Docker Compose, and `bash scripts/setup.sh`. The setup creates an ignored mode-0600 `.env` when needed, applies database schema changes, and starts the app, PostgreSQL 16, and the OpenTelemetry Collector. Credentials are generated from `.env.example` inputs; do not commit `.env`. The app binds to loopback by default and fails closed on blank/placeholder credentials. Database bootstrap, migration owner, and restricted runtime roles remain separate.

The app exports OTLP traces to the Compose collector (`docker/otel-collector.yml`). The Collector currently uses the debug exporter for local verification; this is a development evidence setup, not a persistent production trace backend. The application defaults to 100% sampling locally (`management.tracing.sampling.probability=1.0`). Adjust sampling and exporter endpoints for deployment policy. The app/collector use the same Compose network; the app endpoint is configurable through `OTEL_EXPORTER_OTLP_ENDPOINT`.

## Runtime behavior

Mutations write claim state, claim audit, system audit, idempotency receipt, payment, and outbox evidence in a PostgreSQL transaction. The outbox dispatcher leases due work after commit, creates a child span using the persisted W3C traceparent, and records delivery/retry state. This challenge build uses only mock effect delivery. Correlation ID remains a separate bounded value; HTTP request logs include correlation ID plus validated inbound trace/parent IDs. Outbox logs include active trace/span IDs.

Public health output hides details. Other actuator routes, including Prometheus, require authentication. Do not expose debug collector output on an untrusted network.

## Verification evidence and limits

The latest full clean quality run passed 126 unit and 9 PostgreSQL/Testcontainers tests; six Liquibase changesets applied and JaCoCo passed at 97.0134% line / 90.2022% branch. The combined 29-case live API matrix, 17-case Chrome UI flow, 7-case two-member isolation check, smoke, Postman collection, and same-run HTTP-to-outbox trace chain have current evidence. Final-source seven-profile JMeter runs remain open. See [`docs/testing/README.md`](../testing/README.md), [`reports/verification/REQ-017-current.json`](../../reports/verification/REQ-017-current.json), and [`reports/telemetry/REQ-017-trace-verification.json`](../../reports/telemetry/REQ-017-trace-verification.json).

Docker socket and loopback access are restricted in the default sandbox. Approved escalated invocations were used; permissions and daemon settings were not changed. The final-source seven-profile performance suite has not been rerun. Host-managed raw LLM payloads remain `NOT_CAPTURED`, with audit status `PARTIAL`; full historical capture is a mandatory merge gate. The project remains in progress and not accepted.
