# Challenge 14 implementation contracts

Version: `CH14-Contracts-v1` with coordinator addendum `AD-01`, frozen for implementation on 2026-10-17.

## Runtime boundary

- One Spring Boot 3.5.16 / Java 21 service, PostgreSQL 16, MyBatis 3.0.5 and Liquibase.
- Standard resource layout: `classpath:db/changelog/db.changelog-master.yml`; mapper XML at `classpath:sql/service/claim/*.xml`; workflow YAML at `classpath:config/workflow.yml`.
- Runtime JDBC role defaults to `claims_runtime`; Liquibase role defaults to `claims_migration`. Compose must provision separate credentials. Never embed a usable secret in source or templates.
- HTTP API base is `/api/v1`; authentication uses HTTP Basic for the local Challenge 14 environment. Human demo identities are `clerk`, `lead`, `assessor`, `finance`, and `member-<memberId>`; workflow roles are derived from the authenticated principal. Protected system identity is `system`, with a separate credential. Request fields/headers cannot select an actor, role, or system identity.
- Browser state-changing requests require a valid CSRF token. Actuator health is public with details hidden; other actuator endpoints, including Prometheus, require authentication.

## Frozen service/API types

`ClaimUseCases` is the application boundary. It accepts only domain commands and `Actor`, never HTTP request DTOs:

```java
Claim create(CreateClaimCommand command, Actor actor, String idempotencyKey, String correlationId);
Claim get(UUID claimId, Actor actor);
List<WorkflowOption> options(UUID claimId, Actor actor);
PageResult<Claim> listVisible(PageRequest page, Actor actor);
Claim updateFacts(UUID claimId, UpdateFactsCommand command, Actor actor, String idempotencyKey, String correlationId);
Claim acknowledgeRejection(UUID claimId, long expectedVersion, Actor actor, String idempotencyKey, String correlationId);
TransitionResult transition(UUID claimId, TransitionCommand command, Actor actor, String idempotencyKey, String correlationId);
TransitionResult systemTransition(UUID claimId, TransitionCommand command, Actor actor, String idempotencyKey, String correlationId);
PageResult<AuditRecord> audit(UUID claimId, PageRequest page, Actor actor);
```

API DTOs remain in `claim/api/dto`; transport maps `TransitionResult` to `TransitionResultView { claim, audit, replayed }`. Create and response policy limits are nonnegative PostgreSQL `NUMERIC(18,2)` values: at most 16 integer digits and 2 fractional digits. Currency is exactly three uppercase ASCII letters. Facts are bounded JSON objects with recursively immutable domain representation.

Collection reads use zero-based page (default 0) and size (default 20, max 100), return `{items,total,page,size,totalPages}`, share count/page predicates, and have deterministic ordering. Member filtering is in SQL. The `X-Correlation-ID` header accepts at most 128 safe ASCII characters from `[A-Za-z0-9._:-]`; invalid or absent values become a generated UUID. Return the effective value in the response and structured logs/MDC. Correlation IDs are distinct from tracing IDs. Mutation routes require an `Idempotency-Key`; identical actor/scope/key/fingerprint requests replay the stored result, while key reuse with a different fingerprint is a stable conflict.

## Workflow boundary

The resource `config/workflow.yml` is frozen under T00. It has eight initial states, nine original edges, ten guard IDs, ten effect IDs, and a maximum of three information requests. Preserve all configured role, guard, effect, and error text. New transitions/states must be expressible from existing configured primitives without adding state-specific engine branches. Guards consume trusted, persisted claim facts and server-owned receipt/payment/appeal metadata. Request-supplied values cannot spoof a server-stamped receipt or payment confirmation.

The engine is pure with respect to persistence and HTTP. It determines available choices and validates a requested configured transition. It returns decisions/effect identifiers; the transactional service persists domain changes and durable local outbox/payment/audit records. External mock effects run only after commit.

AD-01 resolves the fact/evidence and persistence seams without changing states,
edges, roles, guard/effect IDs, configured error messages, or the three-cycle
limit. Its exact semantics and domain types are in
[`FACTS-AND-PERSISTENCE.md`](./FACTS-AND-PERSISTENCE.md). The decision originated
from the T01/05 blocked handoff and T02/07 independent review, recorded in the
append-only interaction log.

## Persistence and atomicity

Liquibase 001 creates the base `claims` table. T02 adds schema in a new changeset and updates the master changelog through coordinator integration. Required stores are `claim_audit`, `system_operation_audit`, idempotency/command receipts, payment requests, and local outbox. Successful claim state, domain audit, successful system-operation audit, idempotency result, payment request and outbox changes commit atomically. Claim writes use row locking or version compare-and-set. Audit records are append-only, have no cascading claim-delete relationship, and runtime permissions/triggers deny UPDATE/DELETE. Runtime credentials cannot perform DDL or change migration ownership.

The system-operation audit contains action, resource/resource ID, authenticated actor, correlation ID, outcome, safe error type and timestamp. It never serializes method arguments by default. A successful audited write cannot commit if its audit insert fails. A separately committed failure audit must preserve the original business exception. Idempotency scope includes actor plus operation/resource; fingerprints use a canonical request form and never include credentials.

## Evidence and completion

Worker attempts write only candidate patches and handoffs under their exclusive attempt directories. The coordinator alone applies source patches, runs builds/DB/Docker/Git operations and edits shared docs/manifest/memory. Independent reviewers are read-only. Host-managed model payload fields unavailable from the runtime are `NOT_CAPTURED`; their audit is `PARTIAL`. No application verification, migration success, startup readiness, capacity, or production readiness is claimed without actual evidence.
