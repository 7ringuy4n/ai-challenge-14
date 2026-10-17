# T02 / attempt 13 independent review

**Verdict: REWORK.** The patch is scoped to the five requested persistence integration test files and, by source inspection, its primary claim, audit, receipt, payment, and outbox cases invoke the production MyBatis mapper interfaces and repository adapters. `git apply --check rules/workflow/artifacts/T02/13/candidate.patch` completed successfully (exit 0). However, the shared fixture closes the JDBC connection before it queries migration identity and schema ownership. That makes fixture setup fail before the tests can run. No application or T02 acceptance is implied.

## Findings

1. **[High] Fixture queries a connection already closed by Liquibase.** In `PersistenceITSupport.java:36-45`, the `Liquibase` resource is closed at the end of the try-with-resources block, then lines 46-50 execute a query using the same `migration` connection. Liquibase 4.31.1 `Liquibase.close()` calls `Database.close()`, and `JdbcConnection.close()` closes the underlying JDBC connection. Therefore `migration.createStatement()` at line 46 will fail with a closed-connection error in `@BeforeAll`, preventing every integration test class from initializing. Run the identity/owner assertion before closing Liquibase, or open a fresh connection as `claims_migration` for the assertion.

## Criteria coverage

- **Patch scope/applicability:** PASS; all five paths are under `src/test/java/com/papaya/claims/claim/domain/persistence/`, and `git apply --check` passed.
- **Production mapper/adapters:** PASS by inspection. The fixture loads `classpath:sql/service/claim/*.xml` into `SqlSessionFactoryBean`; claim round-trip/list/CAS uses `MyBatisClaimRepository`; audit append/read uses `MyBatisClaimAuditRepository`; receipt operations use `MyBatisIdempotencyRepository`; payment and outbox paths use their production adapters. Raw JDBC is limited to role/privilege assertions and fixtures/trigger probes.
- **Migration ownership/least privilege:** Intended checks are present, including non-superuser role attributes, runtime schema grants/CREATE denial, and current role/schema owner, but the latter fixture check is unreachable due to Finding 1. The fixture also intentionally uses its bootstrap superuser only to provision roles and transfer schema ownership.
- **CAS immutability/exact decimal:** Claim test calls adapter insert/find/listVisible/compareAndSet, checks nested `BigDecimal`, and attempts to alter persisted member, limit, and currency through the CAS path. Mapper source updates mutable columns only.
- **Audit immutability:** Adapter append/read and raw JDBC UPDATE/DELETE probes cover both audit tables; the runtime role has INSERT but no UPDATE/DELETE grants and append-only triggers are installed by migration 002.
- **Idempotency:** Adapter receipt insert/find and unique-conflict behavior are covered. The packet correctly disclaims service fingerprint comparison, replay policy, and race recovery.
- **Payment/outbox:** Production adapters cover create/exists/confirm/read and enqueue/claim/fail/retry/deliver. The outbox adapter receives an explicit 5000ms lease and the assertion checks the persisted lease window. Application configuration binding itself is not covered.
- **Transactions/autocommit:** MyBatis sessions open with `autoCommit=true`; fixture/JDBC operations use independent connections. This makes the direct privilege-denial probes deterministic. The Liquibase lifecycle issue remains blocking.

## Verification and limitations

Maven/Testcontainers, PostgreSQL runtime behavior, migrations under the fixture, and startup checks are **NOT_RUN**. Per `handoff.json`, the exact deferred command is:

`JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Dit.test=ClaimPersistenceIT,AuditImmutabilityIT,IdempotencyIT,PaymentOutboxIT verify`

Reason: the worker packet prohibited builds/tests and Docker/database operations; coordinator execution is pending after integration. Patch applicability was checked separately as recorded above. The review was read-only and ran no tests, build, Docker, or database commands. Host-managed model payloads remain `NOT_CAPTURED`; audit trace status remains `PARTIAL`.
