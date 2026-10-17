# T02 / attempt 11 independent review

**Verdict: REWORK.** The adapter draft is substantially scoped to the approved paths and maps the six ports, but the claim CAS violates immutable aggregate fields and the sole IT does not establish several advertised persistence/security properties. No build, test, Docker, database, or Git mutation was run; compilation and runtime behavior remain unverified. `PARTIAL` is an accurate handoff/audit status, and this review does not accept T02 or the application.

## Findings

1. **[High] CAS rewrites immutable ownership and policy values.** In `candidate.patch:444-450`, `compareAndSet` sets `member_id`, `policy_limit`, and `currency` from the supplied aggregate while only comparing `id` and `version`. A stale or incorrectly composed aggregate can therefore transfer ownership or change the coverage limit/currency through an ordinary workflow update. Restrict this update to mutable workflow fields (and state/facts as contractually mutable), leaving immutable columns untouched; add an assertion that these fields cannot change through CAS. Schema ownership/values originate in changeset 001 (`src/main/resources/db/changelog/changes/001-initial-schema.sql:4-10`).

2. **[High] IT elevates the Liquibase migration login to SUPERUSER.** `candidate.patch:554-565` provisions `claims_migration` as `SUPERUSER`, then runs Liquibase with it. This bypasses the schema-owner boundary that changeset 002 explicitly assumes (`src/main/resources/db/changelog/changes/002-persistence.sql:87-98`) and cannot verify least-privilege migration/runtime grants. Provision a non-superuser schema owner with only the required ownership/DDL rights, then prove the runtime connection remains limited to its grants.

3. **[Medium] Audit IT does not test append-only protection.** The test at `candidate.patch:590-596` only inserts two rows and checks ordering; it never attempts UPDATE/DELETE under the runtime role or verifies the trigger/grants reject them. Add database assertions for both mutations and retain the existing deterministic ordering assertion. Changeset 002 installs the trigger and grants at lines 77-98.

4. **[Medium] Claimed idempotency/outbox IT coverage is only a happy-path smoke check.** `candidate.patch:598-606` reads back one receipt and claims/delivers one outbox row, but does not test duplicate-key conflict/replay, changed-fingerprint conflict, concurrent `SKIP LOCKED` ownership, retry/error behavior, or payment persistence/confirmation. The packet specifically requires dedicated `AuditImmutabilityIT`, `IdempotencyIT`, and `PaymentOutboxIT`; they are absent, as the handoff also discloses. Add those focused ITs before treating the adapters as persistence-verified.

5. **[Low] Outbox lease is fixed in SQL.** `candidate.patch:473-478` hard-codes a 30-second lease, while failure retry is passed by the caller and configuration currently exposes dispatch/retry delays (`src/main/resources/application.yml`, `claims.outbox`). Make lease duration an explicit configured/bound value or document and contractually fix it; test a lease expiry/reclaim boundary. The handoff correctly flags this as requiring coordinator review.

## Inspected areas

- All declared patch paths are within the task's approved mapper, persistence, SQL-resource, and persistence-test directories; no attempt to edit shared ports/migrations is present.
- Port method shapes align with adapter implementations by inspection. `@Mapper` annotations plus the configured MyBatis starter and mapper resource glob appear consistent; the host application wiring/compilation has not been exercised.
- JSON uses the configured `ObjectMapper`, JSONB text casts, and `use-big-decimal-for-floats`; policy values use `BigDecimal`. This is a plausible exact-decimal path, but the IT cannot prove it until run.
- Claim pagination and count queries use the same member predicate and stable `created_at DESC, id` ordering. Audit reads use sequence/id ordering. The outbox CTE uses `FOR UPDATE SKIP LOCKED` and atomically advances availability/attempts.
- Handoff accurately records missing dedicated IT sources, unrun runtime verification, fixed lease, and host payload capture as `NOT_CAPTURED` / `PARTIAL`.
