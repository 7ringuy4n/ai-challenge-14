# Current Task

## Goal

Implement `CHANGE_REQUEST.md` on the new planning/contracts `main` baseline's `develop` child, preserving honest verification and audit evidence.

## Current status

in_progress — implementation, database integration, startup, smoke and core HTTP lifecycle have passed; the project is explicitly not accepted or complete.

## Current step

Review the complete worktree diff, create reviewed logical implementation commit(s) on `develop`, record hashes, and rerun post-commit state checks.

## Completed

- [x] Inspected Git status, branch graph, remotes, history, and remote refs; origin advertised no refs.
- [x] Preserved the complete pre-baseline worktree at `refs/codex/snapshots/pre-main-final-20261017`.
- [x] Created planning/contracts-only `main` baseline `521cb1427f910ebbd01129cc1a950c55b899ee87`; `develop` descends from it. No app code was added to main, no force/reset/merge/push occurred.
- [x] Resolved Maven plugin dependencies with the configured local build environment; local Java 21/Maven 3.9.11 used. No Maven Wrapper is present.
- [x] Restored workflow validator using `rules/workflow/START.md`; 12 validator tests pass and 13 workflow tasks structurally validate.
- [x] Full clean Maven quality build passed: 120 unit + 9 PostgreSQL 16/Testcontainers tests, 0 failures/errors/skips; all five Liquibase changesets applied.
- [x] JaCoCo gate passed without production exclusions: 892/916 lines (97.3799%) and 549/609 branches (90.1478%).
- [x] Added transaction-bound rollback coverage across claim, claim audit, system audit, idempotency receipt, payment and outbox; added same-key idempotency-lock concurrency and same-version CAS contention tests.
- [x] Independent review found no critical/high findings. Fixed its P2 test-only CAS race and obtained focused rereview confirmation. Advisory-lock 250ms wait sensitivity remains documented.
- [x] Repeated Docker Compose setup passed three times against the existing volume; both app and database stayed healthy, and four claims were present after the final unique scenario run.
- [x] Authenticated API smoke passed; live Python scenario runner passed 20/20 checks through payment confirmation and claim close.
- [x] Captured review/verification/task docs and raw prompt trace; host-managed model payloads remain `NOT_CAPTURED`, audit `PARTIAL`.

## Next exact step

Review the full tracked and untracked diff, stage and commit reviewed implementation/evidence on `develop`, then record commit hash and validate repository/workflow state.

## Remaining open checks

- [ ] Run a single end-to-end suite for all 29 `C14-01`–`C14-29` cases; current live API script has 20 checks, while focused unit/integration tests cover additional business boundaries.
- [ ] Execute JMeter 100-client and 500-client plans; no capacity/SLA claim exists.
- [ ] Validate distributed trace/span propagation; tracing bridge is not configured and current trace/span values are null.
- [ ] Obtain coordinator acceptance after all applicable gates; user explicitly directed that the project remain unaccepted in this continuation.

## Environment note

Default sandbox Docker socket and localhost access are restricted. Docker/Testcontainers and HTTP checks succeeded via approved escalated invocations; no socket permissions, groups, apt locks, or daemon configuration were modified. `.env` is ignored and remains mode 0600.

## Relevant files

- `reports/verification/T08-final.json`
- `reports/sub-agents/T07/final-integrated-review.md`
- `reports/verification/git-history.md`
- `docs/testing/README.md`

## Verification command

```bash
env JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Pquality clean verify
```

## Checkpoint metadata

- Updated (UTC): 2026-10-17T03:22:30Z
- Branch: `develop`
- Commit before implementation commit: `521cb1427f910ebbd01129cc1a950c55b899ee87`
