# Security design and verification

## Identity and authorization

The service uses HTTP Basic with server-side session-backed CSRF protection. The browser fetches a CSRF token before mutation. A domain `Actor` is derived only from the authenticated principal and authorities; request bodies cannot select roles or member identity. The `system` principal must have the protected system authority and is accepted only by the system-transition endpoint. Human mutation methods reject the system identity. Member-scoped claim reads and audit access apply ownership checks, and list/count SQL includes member scope. Runtime database credentials have narrower grants than the Liquibase schema owner.

The in-memory accounts (`clerk`, `lead`, `assessor`, `finance`, `member-demo`, `system`) are local Challenge 14 demonstration identities, not an enterprise identity provider. Human and system passwords come from separate environment variables. Startup fails for empty or `CHANGE_ME_` passwords. Compose binds the HTTP port to loopback by default. TLS termination, external identity, secret rotation, and production account provisioning are not included in this implementation.

## Request and data controls

- JSON parsing rejects unknown fields; JSON request bodies are capped at 65,536 bytes, including requests without a declared content length.
- `X-Correlation-ID` accepts only 1–128 safe ASCII characters. Invalid/missing values get a generated UUID. The exact effective value flows through response, logs, and mutation audit.
- Every mutating claim operation requires an idempotency key; actor, operation/resource scope, canonical fingerprint and response are persisted transactionally.
- Claim rows, immutable audit records, runtime grants and payment/outbox update columns are constrained in PostgreSQL. Domain audit and system-operation audit use append-only mutation triggers.
- Error responses use stable API codes and avoid returning internal SQL or exception details. Failure audit stores the error type, not request bodies, credentials, documents, or unrestricted facts.

## Security verification status

Actor identity derivation, correlation handling, request-size enforcement, and request/resource contract rules have unit coverage. Live API scenarios verified authentication failure, valid session CSRF bootstrap, rejection of missing CSRF, member restrictions, role-bound workflow operations, ownership reads, and audit visibility. PostgreSQL/Testcontainers verified runtime-role grants, append-only audit enforcement, idempotency receipt persistence/serialization, transactional rollback, and concurrent compare-and-set behavior. The independent integrated review found no remaining critical/high implementation issues. These checks are evidence for this challenge implementation, not a security certification; enterprise identity, TLS termination, secret rotation, distributed tracing, and production account provisioning are outside the current scope.
