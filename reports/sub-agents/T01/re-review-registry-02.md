# T01 strategy-registry rereview 02

**Verdict: FAIL for full T01 acceptance/evidence; source review PASS.**

The registry architecture finding is fixed: `GuardEvaluator` resolves guard IDs through its predicate map, `EffectRegistry` resolves effect IDs into pure `EffectIntent`s, the loader validates configuration against those registries, and the engine returns resolved intents without state-name branches. Tests now cover a config-only state/edge extension using existing primitives, intent planning, unknown strategy rejection, configured cycle transition ID, and guard/error behavior. No substantive source defect was identified in the reviewed workflow code/config/contracts.

- **P2 — Frozen config catalog assertions are incomplete.** `WorkflowConfigTest.loadsFrozenWorkflowAndCyclePolicy` checks counts and only that role sets are nonempty; it does not assert the complete expected role/guard/effect IDs/messages.
- **P2 — Spring startup wiring is not tested.** There is no test loading `WorkflowConfiguration` through Spring to evidence resource loading, registry beans, engine wiring, and appeal-period property binding.
- **Acceptance remains pending.** Focused run logged 15 tests, 0 failures/errors/skips; coverage is NOT_MEASURED. Full quality profile and PostgreSQL/Testcontainers checks remain NOT_RUN (Docker daemon denied). The aggregate source hash is stated but packet/JSON do not provide enough hash-procedure detail to reproduce it independently.

Read-only reviewer `/root/t01_review_registry02`, selected gpt-6-luna/medium, actual host model NOT_CAPTURED. Reviewer did not run checks or edit files.
