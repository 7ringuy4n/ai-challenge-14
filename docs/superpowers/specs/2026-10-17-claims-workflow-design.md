# Claims Workflow Orchestrator Design

## Intent and constraints

Build one deployable Java 21 Spring Boot 3.5 service for Challenge 14. It manages claim state transitions from YAML, persists claims and immutable domain/system audit records in PostgreSQL, and dispatches mock effects through a local transactional outbox. The public surface is a secured REST API and dynamic browser UI. Preserve the specified nine transitions, actor/role authorization, information-request cycle limit, and challenge semantics. No broker, second service, JWT, Redis, JPA, or alternative migration system is in scope.

## Architecture

Use feature ownership under `com.papaya.claims.claim.{api,api.dto,service,domain}`. Keep the state-machine interpreter under `workflow`, identity adapters under `security`, framework beans under `config`, cross-cutting errors under `common.web`, append-only system-operation auditing under `audit`, and tracing/correlation under `observability`. Controllers call typed use cases; use cases own transaction boundaries; domain ports isolate MyBatis adapters. `src/main/resources/db/changelog/db.changelog-master.yml` is the only Liquibase entry point, and mapper XML lives under `sql/service/claim/`.

YAML defines states, transitions, roles, guard IDs, effect IDs, and the cycle policy. Registries resolve strategies; the engine contains no state-specific transition switch. A transition transaction checks idempotency and optimistic version, evaluates trusted facts and role, writes claim state plus domain audit and any payment/outbox/receipt rows, and commits atomically. Mock effects run only after commit. `@Auditable` records selected system operations in the same transaction as successful writes; safe failure records may commit independently without replacing the business error. Correlation IDs are validated/generated separately from trace/span IDs and flow through response headers, MDC and audit.

Idempotency keys are scoped by actor and operation/resource. Canonical request fingerprints distinguish replay from key reuse. Database uniqueness and row locking/CAS ensure concurrent identical requests produce one mutation. Claim transition audit remains distinct from supplementary system-operation audit. Both audit stores reject runtime mutation and have database-level protection.

## API and operational contracts

Publish OpenAPI for claim creation, details, visible-claim pagination, facts update, human/system transition, and paginated audit history. Collection reads use zero-based `page` default 0 and `size` default 20/max 100 with typed `{items,total,page,size,totalPages}` responses, identical count/page filters, ownership filtering in SQL, and deterministic ordering. Preserve the stable error envelope. `X-Correlation-ID` accepts bounded safe ASCII or is replaced by a UUID and returned in the response. Prometheus metrics are available at authenticated `/actuator/prometheus`; health details stay hidden. Use one service process and PostgreSQL 16.

## Verification and delivery

Pin compatible Maven/Spring/MyBatis/Liquibase/Testcontainers versions. Quality verification runs unit and PostgreSQL integration tests and JaCoCo with >=95% line and >=90% branch coverage and no business-code exclusions. Docker setup/smoke and five real scenario runs are separate from tests. JMeter 100/500-client plans are reproducible planning artifacts; load runs occur only in a prepared test environment and no capacity claim is made without measured evidence. Every unavailable run is `NOT_RUN` with command and reason. Maintain OpenAPI, architecture and workflow diagrams (editable source plus rendered image), operations/testing docs, Postman assets, dated histories, raw prompt log, and honest partial LLM interaction trace.

## Scope boundaries

The existing candidate patches and historical status fields are unverified drafts. They may inform exploration but are not source or acceptance evidence. The project is currently an unborn local repository with no remote branches; work proceeds on `develop`, created from the empty local `main` state. Host-managed model raw request/response payloads are inaccessible and will be marked `NOT_CAPTURED`, so trace audit status is `PARTIAL`.
