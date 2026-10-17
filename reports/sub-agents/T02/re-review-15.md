# T02 / attempt 14 current-tree independent review

**Verdict: PASS for the standalone test-source patch, subject to the runtime limitations below.** The patch is limited to five new persistence integration test files and, per coordinator-provided evidence, `git apply --check` passed against the current tree. Attempt 13 remains unintegrated; the attempt 14 patch includes its own fixture and adapter-facing tests, so no attempt 13 test files are assumed to exist in the current tree. No T02 acceptance or runtime verification is implied.

## Findings

No blocking source-level findings identified.

- **Patch scope/applicability:** The handoff lists only the five persistence test paths under `src/test/java/com/papaya/claims/claim/domain/persistence/`. Coordinator-recorded `git apply --check rules/workflow/artifacts/T02/14/candidate.patch` is PASS; this reviewer did not run Git.
- **Liquibase fixture lifecycle:** `PersistenceITSupport.provisionRolesMigrateAndWireProductionMappers` closes Liquibase before checking migration identity, then opens a distinct JDBC connection authenticated as `claims_migration`. The `Statement` and `ResultSet` are scoped inside that fresh connection's try-with-resources. This resolves attempt 13 review finding 1; the identity and schema-owner assertion runs on a live migration-authenticated connection.
- **Production adapter coverage:** The fixture loads production mapper XML from `classpath:sql/service/claim/*.xml`, creates mapper proxies through MyBatis, and invokes the repository adapters for claim, audit, idempotency receipt, payment, and outbox behavior. Raw JDBC is limited to role/privilege checks, seed rows, and append-only mutation probes.
- **Claim CAS and mapping:** `ClaimPersistenceIT` exercises insert/find/list/CAS via `MyBatisClaimRepository`, checks nested decimal round-trip, and verifies a CAS update cannot change member ID, policy limit, or currency.
- **Audit immutability and privileges:** `AuditImmutabilityIT` verifies adapter append/read and attempts UPDATE/DELETE against both audit tables through the runtime role, then checks records remain. The role test checks migration/runtime role attributes, schema privileges, and runtime CREATE denial.
- **Idempotency:** `IdempotencyIT` verifies receipt round-trip and unique-key conflict preserves the original receipt. It accurately disclaims service fingerprint/replay/race semantics.
- **Payment/outbox:** `PaymentOutboxIT` covers payment create/exists/confirm/read and outbox enqueue/claim/fail/retry/deliver using production adapters. The configured lease duration is passed explicitly to the adapter and its stored window is checked; application configuration binding itself is not covered.

## Verification and limitations

Build, Maven/Testcontainers execution, PostgreSQL migration/runtime behavior, and application startup checks are **NOT_RUN**. The deferred command recorded in `handoff.json` is:

`JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Dit.test=ClaimPersistenceIT,AuditImmutabilityIT,IdempotencyIT,PaymentOutboxIT verify`

Reason: the worker packet prohibited builds, tests, Docker, and database operations; coordinator execution is pending after source integration. This review was source-only and ran no commands that build, test, connect to Docker, access a database, or mutate Git. Host-managed payloads remain `NOT_CAPTURED`; audit trace status is `PARTIAL`.
