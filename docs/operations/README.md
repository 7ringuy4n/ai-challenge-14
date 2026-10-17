# Operations

## Local environment

Java 21.0.12.1 and Maven 3.9.11 are installed under `/home/tringuyen/.local/java/jdk-21` and `/home/tringuyen/.local/maven`. The normal Maven cache is read-only; checks use `/tmp/claims-m2`. A Maven Wrapper is not present. Since a local Maven installation is available, wrapper bootstrap was not required for this run; a wrapper would still require Java and the Maven distribution/dependencies.

Create `.env` from `.env.example` and use `bash scripts/setup.sh` to generate bootstrap, migration and runtime database passwords before Compose starts. Set `CLAIMS_DEMO_PASSWORD` and a separate `CLAIMS_SYSTEM_PASSWORD`; the application fails closed when either is blank or left at a `CHANGE_ME_` placeholder. Do not commit `.env`. Compose binds the service to loopback by default, runs PostgreSQL 16, grants Liquibase a schema-owner login, and gives the application a restricted runtime login. The bootstrap credential is reserved for first-time database initialization.

The intended sequence is:

```bash
bash scripts/setup.sh
docker compose --env-file .env -f docker/compose.yml up --build -d
bash scripts/smoke.sh
```

Docker daemon access is denied inside the default sandbox, so Docker commands were run only through approved escalated invocations; no socket permissions or daemon settings were changed. `bash scripts/setup.sh` succeeded three times, including the final image rebuild from normalized migration source against the existing volume. Both services reported healthy; existing claim rows were retained (four claims after the final unique scenario run). Liquibase ran five changesets on first startup and reported the database up to date on subsequent startup. The authenticated smoke check and 20-case API scenario run passed after final setup. Evidence is in `reports/verification/compose-repeat-setup.log`, `compose-final-setup.log`, `api-smoke-final.log`, and `e2e-scenarios-final.log`.

## Runtime behavior

Mutations write claim state, claim audit, system audit, idempotency receipt, and any payment/outbox evidence in a PostgreSQL transaction. The scheduled outbox dispatcher runs after commit, applies a configured lease and retry delay, and emits only mock Challenge 14 effects. Outbox delivered/failed counters and request observations are exposed through Micrometer; `/actuator/prometheus` remains authenticated. Public health output hides details.

Logs include method, path, status, duration, effective correlation ID, and trace/span IDs when provided by an observation tracing bridge. The application currently includes Micrometer observations and Prometheus metrics, but no tracing bridge dependency was resolved; observed `traceId` and `spanId` are null in unit output. Do not describe distributed tracing as verified.

## Verification limits

- The default sandbox blocks direct Docker socket and localhost access; approved escalated invocations work. No security controls were modified or bypassed.
- JMeter 100/500-client profiles and distributed trace/span propagation remain unverified. Do not make capacity or tracing claims.
- An earlier first-start DB log contains a `databasechangeloglock` lookup before the table existed; the subsequent successful Liquibase run created all five changesets, and repeat startup reports the database up to date. Current app/database services are healthy. See the preserved container log evidence for exact chronology.
- The project remains in progress and is not marked accepted or production-ready.
