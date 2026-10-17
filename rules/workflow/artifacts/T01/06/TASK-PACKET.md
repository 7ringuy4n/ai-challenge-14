# T01/06 implementation packet — config-driven workflow engine

Task: T01. Requirement links: REQ-20261017-006, -007, -008, -009, -010. Model: gpt-6-luna, reasoning medium. This is a fresh attempt; T01/05 is obsolete and blocked. Dependency T00 contracts are frozen including AD-01, independently PASS reviewed in `reports/sub-agents/T00/ad-01-rereview-03.md`.

Contract version `CH14-Contracts-v1+AD-01`; base source hash `f6844d57f86ba460bdabf30937f8f08da39dbf034cebb357bacbf4d7f19eb1af`.

Read-only inputs: existing `claim/domain` values, `src/main/resources/config/workflow.yml`, `src/main/resources/application.yml`, `docs/challenge-14/CONTRACTS.md`, `docs/challenge-14/FACTS-AND-PERSISTENCE.md`, requirements and quality gates. Patch paths only `src/main/java/com/papaya/claims/workflow/` and `src/test/java/com/papaya/claims/workflow/`. Artifact directory only `rules/workflow/artifacts/T01/06/`; write `candidate.patch` and `handoff.json` there.

Implement immutable YAML parsing/validation, pure configured engine, registered named guard/effect strategies, available transition options and decision results for REST. No state-name engine switch. Reject duplicate/dangling IDs/states/edges, unknown roles/strategies and malformed config. Preserve all existing edges, IDs, roles/messages and three-cycle policy. A test fixture must prove a new state/edge using existing primitives works config-only.

Use `WorkflowEvaluationContext(Claim claim, Actor actor, TransitionCommand command, WorkflowEvidence evidence, Instant now, Duration appealPeriod)`; `WorkflowEvidence` is constructed by the service from persistence and is never request-deserialized. Consume AD-01 fact/guard semantics exactly, including documents keyed by stable ID with field merge and authority enforced by service before evaluation, exact decimals, server-owned request/receive/assessment/rejection/acknowledgement/payment evidence, timestamps at PostgreSQL microsecond precision, and appeal config `claims.workflow.appeal-period` default `P30D`. Engine performs no IO. Configured cycle policy chooses transition ID/counter/exact message; third request succeeds, fourth returns configured exact text.

Add meaningful tests for every guard, roles, cycle boundary, malformed/immutable config, timestamps and config extension. Handoff includes stable engine/API/fact/evidence signatures for T04. Required test command (worker must mark NOT_RUN; coordinator owns execution): `JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Dtest=WorkflowEngineTest,WorkflowConfigTest,GuardEvaluatorTest test`.

Worker may inspect source and draft only. No source/shared edits, tests/builds, Docker/DB/Git mutations, or nested agents. Return candidate.patch and handoff only; report all checks NOT_RUN with exact command/reason.
