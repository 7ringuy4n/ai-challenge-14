# T01 coordinator-remediation rereview 01

**Verdict: FAIL for task acceptance/evidence; PASS for reviewed source remediation findings.**

The earlier P1/P2 logic findings are fixed: malformed roster/boolean fields are rejected and at least one required document is enforced; assessment report must be a map with `complete == true`; acknowledgment must be at or after rejection; duplicate YAML keys are rejected; initial state, positive cycle maximum, roles and cycle policy are validated; configured transition ID/message are used; guard failures carry their IDs. Focused Maven evidence shows 12 tests passed with 0 failures/errors/skips, coverage NOT_MEASURED.

- **P1/P2 — strategy registry architecture remains unimplemented.** `GuardEvaluator.java` uses hardcoded switch dispatch at lines 11–24; `WorkflowConfigLoader.java` contains static allowlists at lines 11–20; effects are passed through as labels in `WorkflowEngine.java` lines 11 and 22 rather than resolved to effect strategies. This conflicts with `FACTS-AND-PERSISTENCE.md` requirement that configured IDs resolve through guard/effect strategy registries.
- **Acceptance evidence remains incomplete.** Required minimum 15 meaningful tests and full quality profile/coverage gates are not met or measured. Database/Testcontainers checks remain NOT_RUN due Docker daemon access. The 12 focused unit tests do not evidence REST, transactions, security, observability, migration/startup, or scenarios.
- Config tests do not assert all expected roles/guard/effect metadata; extension test checks loader acceptance but not dynamic engine behavior. No Spring wiring test covers `WorkflowConfiguration`.

Read-only review; no tests/build run. Reviewer `/root/t01_rereview_remediation`, selected gpt-6-luna/medium; actual model NOT_CAPTURED.
