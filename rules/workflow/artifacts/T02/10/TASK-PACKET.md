# T02/10 coordinator persistence implementation packet

Requirements: REQ-20261017-006 through -010. Contract: CH14-Contracts-v1+AD-01. Current task starts after T00 and scoped T01 acceptance. T02/09 is rejected evidence only.

Implement complete coordinator-owned persistence: typed domain ports/data, PostgreSQL migrations with no duplicate columns from 001, separate migration/runtime role grants, MyBatis mapper/XML adapters, transaction-compatible SQL, idempotency/payment/outbox primitives, and real Testcontainers integration tests. Do not claim Testcontainers passed without Docker. Keep application-owned interactions in English. Current branch is unborn `develop`; no main/origin history exists and no Git history operations are authorized or required for these source edits.

Before writing, inventory current code, schema and configuration. No edits outside this exclusive attempt plan are assigned to another writer; coordinator is the sole source writer. Database/migration, full integration, runtime tests may be marked NOT_RUN with exact reason if Docker remains unavailable. Preserve raw prompts, requirement, interaction and progress evidence.
