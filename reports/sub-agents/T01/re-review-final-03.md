# T01 current-source independent rereview

**Verdict: PASS for the scoped T01 source and focused evidence.** The coordinator still owns final task acceptance. T01 source hash independently recomputed from the listed files is `70388cbebd58b763b596557d6ccc5a67b507726b4b6edc96cb41ad3161db7570`, matching the saved verification report.

## Criteria coverage

- **Config-driven workflow and registries — PASS.** The YAML defines the eight initial states and nine expected edges at `src/main/resources/config/workflow.yml:10-105`. `WorkflowConfigLoader` checks state/edge references, duplicate IDs/edges, known guard/effect strategies, role names and cycle policy (`src/main/java/com/papaya/claims/workflow/WorkflowConfigLoader.java:25-61`). Engine selection is based on configured edges; no state-name switch is used (`WorkflowEngine.java:21-43`).
- **Guard and cycle behavior — PASS.** Guards are resolved via the predicate registry (`GuardEvaluator.java:9-31`), including strict required-document shape/identity validation (`GuardEvaluator.java:65-78`), exact decimal bounds (`:57-60`), and the rejection-acknowledgement timestamp boundary (`:49-55`). The cycle transition, counter, maximum and exact escalation message come from configuration (`WorkflowEngine.java:39-41`; `workflow.yml:5-9`).
- **Pure effect plans — PASS.** Effect IDs resolve through `EffectRegistry` into immutable typed `EffectIntent`s (`EffectRegistry.java:8-45`; `EffectIntent.java:6-11`); engine evaluation returns plans without persistence or external I/O (`WorkflowEngine.java:42-43`).
- **Options/errors and wiring — PASS.** Options include roles, guard/cycle failures and effect identifiers (`WorkflowEngine.java:21-25,45-52`). Invalid edges include source, target and configured choices (`:28-35`); role and guard failures have distinct codes (`:37-41`). Spring wiring loads the configurable resource and binds a positive appeal duration (`WorkflowConfiguration.java:18-61`).
- **Contract and regression evidence — PASS for scoped criteria.** Contract semantics align with the workflow source in `docs/challenge-14/CONTRACTS.md:31-35` and `docs/challenge-14/FACTS-AND-PERSISTENCE.md:37-51`. Config tests exercise a working config-only extension and malformed/unknown strategy rejection (`WorkflowConfigTest.java:84-114`), and the Spring context test checks beans/resource/property binding (`:126-139`).

## Source findings

No actionable source defect found in the reviewed T01 scope. No code changes are requested.

## Evidence and status

- Saved focused Maven command exited 0: **16 tests, 0 failures, 0 errors, 0 skipped** (`reports/verification/T01/focused-maven.log`, summarized in `focused-maven.json`). Saved full unit command exited 0: **27 tests, 0 failures, 0 errors, 0 skipped** (`reports/verification/T01/all-unit-tests.log`). This review did not run tests or builds.
- T01 review result is PASS, pending the coordinator's acceptance record. Whole-application acceptance remains **not accepted**: saved JaCoCo counters are **302/344 lines = 87.79%** and **187/294 branches = 63.61%**, below 95%/90% gates (`focused-maven.json`; `jacoco.xml`). T01 package itself reports 98.46% lines and 74.38% branches; configuration package reports 78.57% lines and 50% branches.
- PostgreSQL/Testcontainers, application startup, migration, Docker setup/smoke, and `mvn -B -Pquality clean verify` remain **NOT_RUN**. The report records Docker daemon access as denied and full integrated acceptance as unavailable; see `focused-maven.json`.
- Evidence would be clearer with a direct terminal-state/no-options assertion for C14-24; the source config currently has no outgoing CLOSED edge. This is a test-evidence gap, not a defect in the reviewed engine implementation.
- Host-managed model request/response payloads remain `NOT_CAPTURED`; audit status is PARTIAL. This review does not claim complete tracing, whole-application verification, or production readiness.

Read-only review; no builds/tests, Docker/database access, or Git mutations performed.
