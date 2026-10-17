# Acceptance scenarios

`bash scripts/run-scenarios.sh` drives the running application over HTTP and writes one JSON file per scenario under `reports/verification/scenarios/`. It reads `.env` as data, or accepts `HOST_PORT`, `DEMO_PASSWORD`, `SYSTEM_PASSWORD`, `BASE_URL`, `ENV_FILE`, and `SCENARIO_OUTPUT` overrides. It does not export credentials, authorization headers, cookies, or CSRF tokens. Each client has an isolated cookie jar.

The script expects the Docker application to be running. Start it with `bash scripts/setup.sh` followed by `docker compose --env-file .env -f docker/compose.yml up -d --build --wait`; then run `bash scripts/smoke.sh` and `bash scripts/run-scenarios.sh`. Setup and scenario execution are separate operations.

The acceptance integration test uses a real PostgreSQL Testcontainer and a random-port Spring Boot server. `PostgresPersistenceSupport` creates the owner/runtime roles and applies the migrations before the Spring context starts; Spring Liquibase then observes the completed changelog. Database readback verifies durable state and audit rows. Docker availability is required to execute it.

Coordinator evidence: all commands are `NOT_RUN` by the worker. Record final command, exit code, source hash, test counts, measured JaCoCo coverage, and scenario output paths under `reports/` after execution. No performance or capacity claim is made.
