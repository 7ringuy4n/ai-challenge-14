# T02 implementation packet — PostgreSQL/MyBatis persistence

Task T02 depends on accepted T00. This is the exact task contract; do not use previous unverified attempts.

Requirement IDs: REQ-20261017-006, REQ-20261017-007, REQ-20261017-008, REQ-20261017-009.
Contract: CH14-Contracts-v1; accepted source hash f6844d57f86ba460bdabf30937f8f08da39dbf034cebb357bacbf4d7f19eb1af; contract SHA-256 77eb655cd9afd38454f467930be37cd177b9a17ed32c6d4814a431694de64e51.
Acceptance cases: C14-13, C14-16, C14-17, C14-18, C14-20, C14-21.
Read-only inputs: base schema 001, master changelog, application.yml, frozen ClaimUseCases/domain values, contract and quality gates.
Patch paths only: seven root domain port/record files listed in manifest; `claim/domain/mapper/`; `claim/domain/persistence/`; `src/main/resources/sql/service/claim/`; only `src/main/resources/db/changelog/changes/002-persistence.sql`; tests under `src/test/java/com/papaya/claims/claim/domain/persistence/`.
Artifact directory: `rules/workflow/artifacts/T02/07/`; write `candidate.patch` and `handoff.json` only there.
Coordinator IT command: `JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH /home/tringuyen/.local/maven/bin/mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Dit.test=ClaimPersistenceIT,AuditImmutabilityIT verify`.

Preserve the base claims table; add claim transition audit, append-only system-operation audit, idempotency/command receipts, payment requests and local outbox. Preserve BigDecimal exactly in JSONB. Grant runtime only needed data access; deny runtime audit UPDATE/DELETE and DDL; add owner trigger defense and no audit cascade deletion. Add uniqueness/indexes for actor+scope+idempotency key, payments and due outbox. Use explicit columns, bound params, no unsafe SQL interpolation, identical ownership predicates for count/page, deterministic order, CAS/locking and affected-row checks, canonical fingerprint/result replay and stable key conflict. Provide typed root domain repository ports using allowed filenames; mapper interfaces under domain/mapper, typed rows/type handlers/adapters under domain/persistence, XML under sql/service/claim. Writes must compose in one transaction. Testcontainers IT sources cover claim roundtrip/pages/member filtering, version contention/rollback, audit immutability/runtime grants, idempotency, payment/outbox retry and exact decimal JSONB. Worker does not run builds/Docker; record checks NOT_RUN. Do not edit master changelog; request coordinator include for changeset 002 in handoff.

Worker protocol: draft only in this attempt directory; no source/shared edits, builds/tests, Docker/DB/Git mutations or nested agents. Handoff lists all repository/mapper signatures, schema objects, hashes, patch paths, criteria/tests, NOT_RUN checks with reason, decisions/risks and master include request. Return only status, artifact paths, concise summary and concerns.
