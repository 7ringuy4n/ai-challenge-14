# T00 fresh independent rereview — PASS

Reviewer: fresh Luna reviewer, medium reasoning. Task: T00, coordinator-03. Source hash: recorded in `reports/verification/T00-contract-check.json`.

## Criteria evidence

- **Eight states/nine configured edges/cycle limit:** `src/main/resources/config/workflow.yml:10-18` lists eight states; lines 51-105 contain exactly the nine original edges with expected roles, guards and effects. Lines 4-9 set the maximum to 3 and preserve the exact escalation message.
- **Liquibase include/resource and credential separation:** `db.changelog-master.yml:1-4` includes the existing SQL resource relative to the changelog; `application.yml:10-13` points at the required classpath and uses separate migration/runtime credentials.
- **OpenAPI mutation schemas:** update-facts and transition bodies/responses are present at `docs/api/openapi.yaml:54-93`; components at lines 197-217 align with request DTOs, and Claim at lines 177-186 aligns with `ClaimView` and `ClaimUseCases`.
- **Immutable transport values:** `ClaimView.java:12-18` copies facts through `ClaimFacts`, which recursively protects maps/lists (`ClaimFacts.java:18-39`); regression test is `ClaimViewTest.java:14-27`.
- **Build profile pins:** Java 21 and JaCoCo thresholds are present in `pom.xml:16-20,55-78`.
- **Test evidence:** reviewer inspected reports; 8 tests, 0 failures/errors/skips. Reviewer did not execute commands.

## Advisory documentation finding

`docs/architecture/README.md:5` still claimed Java/Maven were absent and T00 blocked. This was a stale project status statement. Coordinator corrected the documentation after review.

## Commands not run by reviewer

Build/tests, Docker, DB, Git and source hash recomputation. Full application quality, coverage, database migration and startup checks remain future gates.
