# Claims Workflow Orchestrator Implementation Plan

> **For agentic workers:** Required subskill: subagent-driven-development; project workflow assigns patches to workers and keeps source integration with the coordinator.

**Goal:** Deliver a locally runnable, tested, documented Challenge 14 claims workflow service with truthful evidence.

**Architecture:** One Spring Boot/PostgreSQL deployable with a YAML state machine, transactional service/domain boundaries, MyBatis/Liquibase persistence, security, paged REST, browser UI, correlation/metrics, and local outbox. Coordinator freezes contracts, workers draft isolated patches, coordinator integrates serially, and independent reviewers gate acceptance.

**Tech Stack:** Java 21, Maven, Spring Boot 3.5.16, Spring Security, MyBatis 3.0.5, PostgreSQL 16, Liquibase, JUnit 5, Mockito, AssertJ, Testcontainers, JaCoCo, Docker Compose, JMeter.

**Spec:** `docs/superpowers/specs/2026-10-17-claims-workflow-design.md`

## Global Constraints

- Keep one deployable Java/PostgreSQL application and use the stated versions.
- Preserve the Challenge 14 eight states, nine original edges, role rules, guards, and maximum three information cycles.
- Keep domain transition audit and supplementary system-operation audit distinct and append-only.
- Commit successful mutation, audit, receipt, payment, and outbox rows atomically.
- Derive actor from the authenticated principal; never trust request-supplied identity.
- Keep raw human prompts verbatim and mark inaccessible host payloads `NOT_CAPTURED`.
- Coordinator is the only source/shared-file writer; worker patch paths are exclusive.
- Never report unexecuted checks or unmeasured capacity as success.

## Review Focus

- Concurrent identical idempotency requests must produce one persisted mutation and one effective audit event.
- Member claim filtering must happen in SQL for both count and page reads.
- Runtime credentials must not mutate or delete either audit store.
- Bad correlation input must not be reflected unsafely into headers or logs.
- `@Auditable` must execute through Spring proxies inside the intended transaction boundary.

---

### Task 0: Freeze contracts and build

**Files:** POM, application YAML, bootstrap, DTO/error contracts, OpenAPI, architecture contract, test fixtures, Postman skeleton.

**Produces:** Java 21 / Spring 3.5.16 / MyBatis 3.0.5 build, API/value/error contracts, classpath paths, security identities, datasource/migration credential split, quality profile.

- [ ] Pin dependency and plugin versions and configure unit/integration/JaCoCo gates.
- [ ] Define API/DTO/error, identity, correlation, pagination and migration contracts.
- [ ] Document transaction/idempotency/outbox/audit boundary and route semantics.
- [ ] Run `mvn -B test`; record PASS or exact NOT_RUN reason.

### Task 1: YAML workflow engine

**Files:** `workflow/**`, `config/workflow.yml`, workflow tests.

**Consumes:** T0 guard/effect IDs and claim fact vocabulary.
**Produces:** immutable workflow definition, loader, validation, engine and strategy registries.

- [ ] Test all configured transitions/guards, errors, cycle boundary, invalid definitions and config-only extension.
- [ ] Implement config loader and pure engine without state-specific switches.
- [ ] Run focused workflow tests and record actual output.

### Task 2: persistence and migrations

**Files:** `claim/domain/{mapper,persistence}`, Liquibase resources, mapper XML and PostgreSQL ITs.

**Consumes:** T0 schema/DTO contracts.
**Produces:** claim/audit/system-audit/outbox/payment/idempotency schema, mapper ports/adapters and immutable audit enforcement.

- [ ] Add schema and runtime-role grants/immutability triggers.
- [ ] Implement typed MyBatis SQL with CAS, counts/pages, stable ordering and ownership filtering.
- [ ] Verify migrations, rollback, grants and constraints on PostgreSQL Testcontainers.

### Task 3: deployment and environment

**Files:** Docker assets, safe env template, setup/smoke scripts.

**Consumes:** T0 jar/port/health/env contract.
**Produces:** reproducible Compose database/application packaging.

- [ ] Define healthchecks, durable volume, migration/runtime credentials and safe defaults.
- [ ] Validate Compose syntax; runtime smoke is coordinated after integration.

### Task 4: transactional use cases, security and observability

**Files:** `claim/service`, `security`, `observability`, `audit`, configuration, focused service/IT tests.

**Consumes:** T1 engine and T2 ports.
**Produces:** create/update/transition/query use cases, authenticated principal actor, idempotency, audit aspect, after-commit outbox dispatcher, correlation/MDC and metrics.

- [ ] Prove atomicity, authorization, replay/conflict and concurrency on PostgreSQL.
- [ ] Prove audit aspect proxy and transaction behavior; safe failure audit does not replace business errors.
- [ ] Prove dispatch occurs only after commit and retry status remains observable.

### Task 5: REST and browser UI

**Files:** `claim/api`, `claim/api/dto`, `common/web`, static UI and web tests.

**Consumes:** T4 use cases and T0 OpenAPI/error/paging contracts.
**Produces:** secured API, stable errors, dynamically configured UI, paged claims/audit lists.

- [ ] Test route, binding, ownership, validation, CSRF, error envelope and pagination.
- [ ] Render user content safely and load transitions dynamically.

### Task 6: package and scenario acceptance

**Files:** acceptance tests/fixtures, scenario scripts, testing docs.

**Consumes:** T3/T5/T9/T10/T11/T12 as listed in manifest.
**Produces:** five required scenarios, guard/race/config cases, real outputs and quality evidence.

- [ ] Run tests with `running test case N/TOTAL` progress.
- [ ] Run full quality profile, setup, smoke and scenario commands; record outcomes and source hashes.

### Task 7: independent final review

**Files:** review evidence only.

- [ ] Fresh Luna reviewer checks every C14 criterion against final source and actual outputs.
- [ ] Coordinator fixes findings and reruns affected gates.

### Task 8: submission and synchronized history

**Files:** README, docs, histories, reports, submission logs and manifest.

- [ ] Record source-matched build/test/runtime/coverage/load evidence, actual counts, and all NOT_RUN/NOT_CAPTURED entries.
- [ ] Rebuild memory and synchronize progress/changelogs/history. Do not publish remotely.

## Execution notes

The detailed task graph and exact exclusive paths live in `rules/workflow/tasks.json`. Shared contract changes must be made by the coordinator before consumers dispatch. Since this project has no source and the existing worker artifacts are unverified, their patches are not applied automatically.
