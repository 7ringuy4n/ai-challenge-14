# T03 implementation packet — Docker Compose and environment

Task T03 depends on accepted T00. This is the exact task contract; do not use previous unverified attempts.

Requirement IDs: REQ-20261017-006, REQ-20261017-007, REQ-20261017-008, REQ-20261017-009.
Contract: CH14-Contracts-v1; accepted source hash f6844d57f86ba460bdabf30937f8f08da39dbf034cebb357bacbf4d7f19eb1af; contract SHA-256 77eb655cd9afd38454f467930be37cd177b9a17ed32c6d4814a431694de64e51.
Acceptance case C14-28.
Read-only inputs: POM, application.yml, workflow YAML, contract and quality gates.
Patch paths only: `docker/Dockerfile`, `docker/compose.yml`, `docker/initdb/`, `.env.example`, `scripts/setup.sh`, `scripts/smoke.sh`.
Artifact directory: `rules/workflow/artifacts/T03/05/`; write `candidate.patch` and `handoff.json` only there.
Coordinator syntax command: `docker compose --env-file .env.example -f docker/compose.yml config --quiet`. Runtime commands: `bash scripts/setup.sh`; `bash scripts/smoke.sh`.

Package one PostgreSQL 16 and one Spring Boot app. Use durable named DB volume, health checks, localhost-only host binding, restart policy, Java 21 non-root runtime, `claims-workflow-1.0.0-SNAPSHOT.jar`, internal port 8080 and `/actuator/health`. Keep migration/runtime roles `claims_migration` and `claims_runtime`. Match `CLAIMS_DATABASE_URL`, `CLAIMS_RUNTIME_DB_USER`, `CLAIMS_RUNTIME_DB_PASSWORD`, `CLAIMS_MIGRATION_DB_USER`, `CLAIMS_MIGRATION_DB_PASSWORD`, `SERVER_PORT`, `CLAIMS_DEMO_PASSWORD`, and `CLAIMS_SYSTEM_PASSWORD`. Init creates least-privilege runtime role without logging secrets. `.env.example` contains conspicuous dummy values only. Setup creates `.env` only when absent or still dummy, generates strong local secrets, preserves valid existing values, never echoes credentials, validates Compose then starts and waits for health. Smoke performs an authenticated health/API check without passwords/tokens in output. Compose syntax must work without daemon access. No production secret in source.

Worker protocol: draft only in assigned attempt directory; no direct source/shared edits, builds, Docker/DB/Git mutations or nested agents. Handoff lists files/hashes, exact NOT_RUN checks/reasons, env policy, decisions/risks. Return only status, artifact paths, summary and concerns.
