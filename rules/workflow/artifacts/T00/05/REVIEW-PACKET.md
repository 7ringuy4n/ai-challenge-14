# T00 scoped rereview packet — attempt coordinator-05

- Requirement/task: REQ-20261017-006 through REQ-20261017-009 / T00.
- Reviewer role/model: fresh independent reviewer, gpt-6-luna / medium; read-only.
- Source hash (`src/main` + `src/test` aggregate): `f6844d57f86ba460bdabf30937f8f08da39dbf034cebb357bacbf4d7f19eb1af`.
- Prior review: `reports/sub-agents/T00/review-04.md` (two P2 findings).
- Current check: `JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B test`; exit 0; 11 tests, 0 failures/errors/skips. Live progress lines `running test case 1/11` through `11/11` appear in `reports/verification/T00-review04-full-tests.log`. Coverage, runtime, migration and database checks remain unverified.
- Scope: re-review only review-04 finding F-1 (API/domain/view/OpenAPI numeric(18,2) consistency) and F-2 (transition response semantics/schema/mapping test), plus regressions in files changed to resolve them.

## Changed contract files

- `src/main/java/com/papaya/claims/claim/domain/Claim.java`
- `src/main/java/com/papaya/claims/claim/domain/CreateClaimCommand.java`
- `src/main/java/com/papaya/claims/claim/api/dto/CreateClaimRequest.java`
- `src/main/java/com/papaya/claims/claim/api/dto/ClaimView.java`
- `src/main/java/com/papaya/claims/claim/api/dto/TransitionResultView.java`
- `docs/api/openapi.yaml`
- `src/test/java/com/papaya/claims/claim/api/dto/` (request validation, view bounds and transition-result mapping)
- `src/test/java/com/papaya/claims/config/ResourceContractTest.java`
- `pom.xml`, `src/test/java/com/papaya/claims/testing/TestProgressListener.java`, and `src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener` (live N/TOTAL reporting)

## Findings to verify

F-1: Request boundary enforces nonnegative `numeric(18,2)` (16 integer digits and at most 2 fractional digits); domain constructors and response views enforce the same; OpenAPI schemas declare minimum, maximum and `multipleOf: 0.01`; tests reject excess scale/precision and negative values while accepting the maximum.

F-2: Transition use cases return `TransitionResult`; `TransitionResultView` maps claim, audit and replay status; both transition endpoints reference an OpenAPI `TransitionResult` schema with matching `Claim` and `AuditEntry` components; mapping test asserts result values.

Return ADDRESSED / NOT ADDRESSED for each finding, list any new actionable breakage in the changed scope with severity/file:line/trigger/consequence/fix, and give a concise verdict. Do not edit files, run builds, DB/Docker/Git mutations, or spawn agents.
