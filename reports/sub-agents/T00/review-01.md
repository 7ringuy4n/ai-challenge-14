# T00 review — FAIL

Reviewer: independent Luna reviewer, medium reasoning. Task: T00, attempt coordinator-01. Source hash reviewed: `d87676b1625c5563ff9d5dd7b5524ee84bbbade6388f74ea47c55d1ac83497a4`.

The static contracts are partly in place, and the recorded test run passed, but the contract-completeness criterion is not met.

| Criterion | Result | Evidence |
|---|---|---|
| Java 21, Spring Boot 3.5.16, MyBatis 3.0.5 | PASS | Pinned in pom.xml; Java release is 21. |
| Liquibase classpath and separate credentials | FAIL | Configured master path and distinct runtime/migration credential variables are present. The master includes `changes/001-initial-schema.yml`, which is absent, so startup with Liquibase cannot resolve it. |
| Routes, pagination, correlation, idempotency, error shapes | FAIL | OpenAPI describes routes and paging, plus correlation, idempotency and error envelope. Facts-update and transition operations have no request bodies, and mutation success responses omit response schemas. |
| Immutable typed fact/page/domain boundaries | FAIL | `ClaimFacts` recursively copies maps/lists and `PageResponse` copies items. `ClaimView` accepts a mutable Map without copying it; its `from()` method passes through the domain map. |
| No premature implementation claims | PASS | Only the contract-test pass was claimed; coverage is `NOT_RUN`. |

## Test evidence

`reports/verification/T00-contract-check.json` recorded exit 0, 5 tests, no failures/errors/skips, source hash above. Surefire reports show `PageResponseTest` 2 and `DomainContractTest` 3. Assertions cover defensive nested fact copying, page boundaries/metadata and actor role copying. No tests cover OpenAPI/DTO correspondence or Liquibase resource resolution.

## Findings

- **High — unresolved migration include:** add the referenced changeset or correct the include before treating the path as usable. `src/main/resources/db/changelog/db.changelog-master.yml:2`.
- **High — incomplete OpenAPI mutation contracts:** document update-facts and transition request bodies and response schemas. `docs/api/openapi.yaml:46`.
- **Medium — mutable transport value:** defensively copy `ClaimView.facts`, including nested collections. `src/main/java/com/papaya/claims/claim/api/dto/ClaimView.java:8`.

Commands not run: independent reviewer did not run builds, Docker, database, or Git. Full quality, coverage, migration startup, and runtime checks remain unverified.
