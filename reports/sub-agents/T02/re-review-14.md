# T02 / attempt 14 independent review

**Verdict: REWORK.** The lifecycle correction itself addresses attempt 13's finding: after Liquibase closes its supplied connection, the fixture opens a distinct connection as `claims_migration`, queries `current_user` and the `public` schema owner, and closes the result set, statement, and connection in one try-with-resources scope. The attempt-14 patch is limited to the five permitted persistence IT files, and the test content retains the production mapper/adapters and checks described in the packet. However, it is a full five-file `new file` patch copied from attempt 13, not an applicable correction delta on top of the attempt-13 test additions. Once attempt 13 is applied, these paths already exist and the attempt-14 patch cannot apply as written. No build, runtime test, or T02 acceptance is implied.

## Findings

1. **[High] Patch cannot be applied after its declared base attempt.** All five file headers in `candidate.patch` are `new file mode` with `--- /dev/null`. Attempt 13 already adds those same five paths. The attempt-14 packet identifies attempt 13's patch as its base and says to retain those files and change the fixture lifecycle; consequently this patch must be a delta against those files (or otherwise provide a valid, explicit replacement strategy). As written, applying it after attempt 13 encounters existing paths and fails. I did not run `git apply --check` because this review was required to remain read-only and avoid Git operations.

## Criteria coverage

- **Permitted scope:** PASS by patch inspection; only `src/test/java/com/papaya/claims/claim/domain/persistence/` files are present.
- **Fixture correction:** PASS by source inspection. The Liquibase-owned connection is not reused. A fresh migration-login connection verifies `current_user` and `public.nspowner`; JDBC resources are scoped and closed.
- **Adapter-facing coverage:** PASS by source inspection. Claim, claim-audit, idempotency receipt, payment, and outbox operations invoke the production repository adapters backed by MyBatis mapper XML. Raw JDBC is used for role/privilege assertions, seed data, and append-only mutation probes.
- **Rework criteria / applicability:** REWORK. The patch recreates attempt-13 files as additions rather than expressing a patch applicable to those existing files.
- **Least privilege and immutable data:** Intended checks remain present: non-superuser role attributes, runtime schema USAGE without CREATE, failed runtime CREATE TABLE, claim CAS preserving member/policy/currency, and denied audit UPDATE/DELETE attempts.
- **Idempotency and effects:** The receipt test covers insert/find and unique-key conflict; payment and outbox tests cover their adapter paths and the explicit 5000ms lease. The packet appropriately does not claim service replay/fingerprint/race behavior or application configuration binding.

## Verification and limits

Build, Testcontainers, migration execution, database behavior, and startup checks are **NOT_RUN**. The packet's deferred command is:

`JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Dit.test=ClaimPersistenceIT,AuditImmutabilityIT,IdempotencyIT,PaymentOutboxIT verify`

Reason: the review packet prohibits builds/tests and Docker/database operations; coordinator execution remains pending. Patch applicability was assessed by inspecting the declared base and patch headers; no Git command was run. Host-managed request/response payloads remain `NOT_CAPTURED`; audit trace status remains `PARTIAL`.
