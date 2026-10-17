# T02 / attempt 12 independent review

**Verdict: REWORK.** Least-privilege role provisioning is materially improved and the requested cases are represented, but most integration tests issue SQL that reproduces the production mapper statements instead of invoking the current MyBatis mapper/adapters. As a result they do not verify production adapter wiring, parameter binding, row mapping, or repository behavior. Runtime evidence is correctly `NOT_RUN`; this review does not accept T02 or the application.

## Findings

1. **[High] Integration cases bypass the production adapters.** `AuditImmutabilityIT.java:12-23`, `IdempotencyIT.java:14-30`, and `PaymentOutboxIT.java:14-56` use handwritten JDBC inserts/updates and SQL. `PersistenceITSupport.java:52-65` also creates fixtures directly. The outbox claim CTE in `PaymentOutboxIT.java:36-39` hard-codes a 30-second lease and SQL shape, rather than calling `MyBatisOutboxRepository.claimDue` / `EffectsMapper.claimDue` (the current mapper binds `leaseMillis` at `src/main/resources/sql/service/claim/EffectsMapper.xml`, `claimDue`). This can pass while mapper SQL, MyBatis bindings, adapters, or Spring wiring are broken. Wire the tests to the production mapper/adapters (or explicitly scope these as schema/privilege SQL tests and add adapter-facing assertions).

2. **[Medium] Idempotency assertions cover storage constraints only.** `IdempotencyIT.java:14-30` verifies one receipt, JSONB equality, and that a duplicate insert throws `SQLException`; it does not invoke `MyBatisIdempotencyRepository.find/insert` or test the adapter's domain-object mapping. It correctly does not claim fingerprint mismatch/replay policy. Add adapter-facing round-trip and duplicate-key behavior checks while keeping service fingerprint comparison explicitly out of scope.

3. **[Medium] CAS regression is partly textual and the database mutation is not CAS.** `ClaimPersistenceIT.java:33-40` inspects the packaged XML for the absence of immutable column names in the `<update>`, while `:44-65` runs a handwritten state/version update. Neither executes `ClaimMapper.compareAndSet` / `MyBatisClaimRepository.save` with deliberately altered immutable values. Keep the XML contract check if useful, but exercise the production CAS path and verify member ID, policy limit, and currency remain unchanged.

4. **[Low] Migration role assertions omit actual schema ownership.** `ClaimPersistenceIT.java:11-30` verifies role attributes and schema privileges, and the fixture does connect to Liquibase using migration credentials (`PersistenceITSupport.java:31-38`), but it never asserts `public` is owned by `claims_migration` or that Liquibase ran as that role. Add a direct `pg_namespace` owner/current-user assertion so the least-privilege migration boundary is observable.

## Criteria coverage

- Audit UPDATE/DELETE denial on both tables and row survival: represented, but raw SQL only (`AuditImmutabilityIT.java:10-26`).
- Receipt uniqueness, fingerprint and result persistence: represented at database level; no repository adapter call (`IdempotencyIT.java:11-31`).
- Payment creation/existence/confirmation: represented via raw SQL (`PaymentOutboxIT.java:12-31`).
- Outbox claim/lease/retry/delivery: represented via raw SQL; no concurrent `SKIP LOCKED` or adapter exercise (`PaymentOutboxIT.java:33-57`).
- CAS immutable fields: XML text contract plus non-CAS SQL; production CAS path untested (`ClaimPersistenceIT.java:33-65`).
- Least privilege: role flags, grants, and runtime CREATE denial are asserted; schema ownership/current migration identity are not asserted.

## Runtime verification

Maven/Testcontainers and PostgreSQL runtime checks are **NOT_RUN**, as disclosed in `handoff.json`. The exact command recorded there is `JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Dit.test=ClaimPersistenceIT,AuditImmutabilityIT,IdempotencyIT,PaymentOutboxIT verify`; reason: worker instructions prohibited builds/tests, Docker and database operations, pending coordinator integration. `git apply --check` is recorded PASS; no runtime conclusion follows from it.
