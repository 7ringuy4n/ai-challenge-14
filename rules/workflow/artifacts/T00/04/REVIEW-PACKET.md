# T00 contract re-review packet — attempt coordinator-04

- Task/requirement: T00 / REQ-20261017-006 through REQ-20261017-009
- Role: independent read-only reviewer; use gpt-6-luna / medium.
- Contract version: T00-v2 (2026-10-17).
- Source hash (src/main + src/test aggregate): `b1bc449f5eaccba07ebbd1aa7969ddc15d4863db0a2388a659d074a0badebe27`.
- Current focused command: `JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B test` (exit 0; 8 tests, 0 failures/errors/skips).
- Output: `reports/verification/T00-rerun-20261017.log`; machine record: `reports/verification/T00-contract-check-current.json`.

## Review criteria

1. Verify Java 21/Spring Boot 3.5.16/MyBatis 3.0.5 build pins and required quality gates.
2. Verify the 8 workflow states, 9 configured transitions, expected role/guard/effect metadata, cycle maximum 3, and exact escalation message in `src/main/resources/config/workflow.yml`.
3. Verify the Liquibase classpath include resolves and application config separates migration and runtime credentials.
4. Verify REST transport request/response schemas, paging/correlation/idempotency/error contracts in `docs/api/openapi.yaml` align with actual DTOs/use-case signatures. Transition use cases return `TransitionResult`; transport may intentionally return `Claim` if documented and covered.
5. Verify immutable nested facts and page/domain values, and ensure policy limit/currency fields are present consistently across Claim/domain/API contract.
6. Verify current compile/test evidence is on the exact source hash above. Do not claim coverage/runtime/migrations/startup.

## Files to inspect

`pom.xml`, `src/main/java/com/papaya/claims/claim/domain/`, `src/main/java/com/papaya/claims/claim/service/ClaimUseCases.java`, `src/main/java/com/papaya/claims/claim/api/dto/`, `src/main/resources/application.yml`, `src/main/resources/config/workflow.yml`, `src/main/resources/db/changelog/`, `docs/api/openapi.yaml`, `src/test/java/com/papaya/claims/`, and `reports/verification/T00-contract-check-current.json`.

Return PASS/FAIL, criterion-by-criterion evidence and actionable severity/file:line/trigger/consequence/fix findings. Read only; do not edit files, run builds/DB/Docker/Git mutations, or spawn agents. The coordinator owns acceptance.
