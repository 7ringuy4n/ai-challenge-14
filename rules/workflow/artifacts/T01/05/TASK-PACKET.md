# T01 implementation packet — config-driven workflow engine

Task T01 depends on accepted T00. This is the exact task contract; do not use previous unverified attempts.

Requirement IDs: REQ-20261017-006, REQ-20261017-007, REQ-20261017-008, REQ-20261017-009.
Contract: CH14-Contracts-v1; accepted source hash f6844d57f86ba460bdabf30937f8f08da39dbf034cebb357bacbf4d7f19eb1af; contract SHA-256 77eb655cd9afd38454f467930be37cd177b9a17ed32c6d4814a431694de64e51.
Acceptance cases: C14-01, C14-05, C14-06, C14-07, C14-08, C14-09, C14-10, C14-11, C14-12, C14-14, C14-15, C14-22, C14-23, C14-24.
Read-only inputs: `src/main/resources/config/workflow.yml`, `src/main/java/com/papaya/claims/claim/domain/`, `docs/challenge-14/CONTRACTS.md`, design spec and quality gates.
Patch paths only: `src/main/java/com/papaya/claims/workflow/` and `src/test/java/com/papaya/claims/workflow/`.
Artifact directory: `rules/workflow/artifacts/T01/05/`; write `candidate.patch` and `handoff.json` only there.
Coordinator verification after applying: `JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Dtest=WorkflowEngineTest,WorkflowConfigTest,GuardEvaluatorTest test`.

Implement immutable YAML parsing/validation, a pure configured engine, named guard/effect strategies, and transition options/decision results for REST consumption. No state-name-specific engine switch. Fail clearly on duplicate/dangling IDs/states/edges, unknown guard/effect/role IDs, malformed operands. Preserve all nine production edges, guard/effect IDs, roles, messages and cycle policy. Enforce three information requests and the exact fourth-attempt text. Distinguish invalid edge, unauthorized role and each guard failure; return available transitions. Guards consume trusted persisted claim/receipt/payment/appeal facts, distinguish request facts from server-stamped facts, and use injected Clock with PostgreSQL microsecond precision. Demonstrate a new state/edge only in a test fixture with existing primitives. Add meaningful tests for each guard, roles, cycle boundary, malformed config, immutability, timestamp boundaries, invalid edges and extension. Return stable public API signatures and fact vocabulary in handoff; T04 consumes them only after accepted review.

Worker protocol: draft only in this attempt directory. Do not edit application files/shared plans/memory/build config, run builds/tests, Docker/DB/Git mutations, or spawn agents. Handoff includes task/attempt/model/effort/contract/base hashes/patch paths/criteria/tests/NOT_RUN checks/decisions/risks/shared changes. Report every check NOT_RUN with exact command/reason. Return only status, artifact paths, concise summary and concerns.
