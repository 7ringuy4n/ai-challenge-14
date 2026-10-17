# T01/07 independent review

**Verdict: FAIL; unsafe to integrate unchanged.**

The eight production classes are present and the patch/handoff match. Collection records defensively copy their contents, evidence timestamps are truncated to microseconds, payment guards consume server evidence, and the engine performs no I/O. The candidate is PARTIAL and has no tests, no measured coverage, and required Maven command NOT_RUN.

## Findings

- **P1 — Document guard accepts malformed/incomplete rosters.** `GuardEvaluator.java:26-33` only checks nonempty map and fields for entries marked required. It does not validate stable ID grammar, boolean presence/types, or at least one required entry; a nonempty map with no valid required document can pass.
- **P1 — Appeal acknowledgement bypasses timestamp ordering.** `GuardEvaluator.java:25` accepts any non-null acknowledgement without requiring it to be at/after rejection.
- **P1 — Assessment-report guard uses the wrong fact shape.** `GuardEvaluator.java:18` accepts any nonblank string; contract requires an object whose `complete` field is exactly true.
- **P2 — Cycle policy is partly hardcoded.** `WorkflowConfigLoader.java:20-21` reads only maximum and error-message, omitting transition-id and counter. `WorkflowEngine.java:17` hardcodes `request-information`.
- **P2 — Duplicate YAML keys are not rejected.** The default mapper in `WorkflowConfigLoader.java:12` can silently overwrite duplicate keys.
- **P2 — Guard errors are not distinct.** `WorkflowEngine.java:15` reports generic `GUARD_FAILED` for every guard.
- **P2 — Loader validation is incomplete.** It does not confirm the initial state exists, validate positive cycle maximum, or reject unknown roles.

## Acceptance gaps

No production behavior is tested. Missing tests include config validation/extension; positive and negative paths for every guard; role and invalid edge; third/fourth cycle boundary; microsecond timestamp boundaries. Application wiring/config binding is absent. Handoff correctly records PARTIAL and NOT_CAPTURED host payloads. No tests/builds were run by reviewer.

Reviewer `/root/t01_review07`, selected gpt-6-luna/medium; actual host model NOT_CAPTURED.
