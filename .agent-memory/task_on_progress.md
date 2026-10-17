# Current Task

## Goal

Implement `CHANGE_REQUEST.md` on the new planning/contracts `main` baseline's `develop` child, preserving honest verification and audit evidence.

## Current status

in_progress — implementation, quality, PostgreSQL, startup, smoke, core API lifecycle, validator, and independent-review checks pass; the project is not accepted or complete.

## Current step

Continue the unified live acceptance matrix and available load/tracing checks; retain open status until coordinator acceptance.

## Completed

- [x] Inspected Git status, branch graph, remotes, history and remote refs; origin advertised no refs.
- [x] Preserved pre-baseline worktree at `refs/codex/snapshots/pre-main-final-20261017`.
- [x] Created planning/contracts-only `main` baseline `521cb1427f910ebbd01129cc1a950c55b899ee87`; `develop` descends directly from it. No app code on main and no force/reset/merge/push occurred.
- [x] Resolved Maven plugin dependencies with the configured build environment; local Java 21/Maven 3.9.11 used. No Maven Wrapper is present.
- [x] Restored workflow validator using `rules/workflow/START.md`; 12 validator tests pass and 13 manifest tasks structurally validate.
- [x] Final clean quality build passed 120 unit + 9 PostgreSQL 16/Testcontainers tests, 0 failures/errors/skips; all five Liquibase changesets applied.
- [x] JaCoCo gate passed without production exclusions: 892/916 lines (97.3799%) and 549/609 branches (90.1478%).
- [x] Added PostgreSQL rollback coverage across claim, claim audit, system audit, idempotency receipt, payment and outbox; added same-key idempotency-lock concurrency and same-version CAS contention tests.
- [x] Independent review found no critical/high findings. Fixed its P2 CAS test race and received focused rereview confirmation. Advisory-lock 250ms wait sensitivity remains documented.
- [x] Repeated Compose setup three times against the existing volume; final app and DB healthy, prior rows persisted, and four claims remained after the final unique scenario run.
- [x] Authenticated API smoke passed; live Python scenario runner passed 20/20 checks through payment confirmation and claim close.
- [x] Updated requirements, task manifest, history/changelogs, verification evidence, raw prompts and LLM review audit. Host-managed model payloads remain `NOT_CAPTURED`, trace audit `PARTIAL`.
- [x] Committed reviewed implementation on `develop`: `207c70d7e559af9be86fb5cf9e9b538a42b8bf22` (parent `521cb1427f910ebbd01129cc1a950c55b899ee87`); no push or merge.
- [x] Committed final verification/Git documentation as `cd4032b2e25940df261b0dfca301429a54fe87ec`.
- [x] Post-commit status clean; `main` remains at `521cb1427f910ebbd01129cc1a950c55b899ee87`, the direct `develop` parent.

## Remaining open checks

- [ ] One unified live API suite for all 29 `C14-01`–`C14-29` cases. Current live runner has 20 checks; additional business boundaries have focused unit/integration coverage.
- [ ] JMeter 100-client and 500-client plans. `jmeter` is not installed and no `.jmx` profile exists.
- [ ] Distributed trace/span propagation. No tracing bridge is configured; observed trace/span values are null.
- [ ] Coordinator acceptance; user explicitly directed this continuation remain unaccepted.

## Environment note

Default sandbox Docker socket and localhost access are restricted. Testcontainers, Compose and HTTP checks passed through approved escalated invocations. No socket permissions, groups, apt locks, or daemon settings changed. `.env` is ignored and mode 0600.

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

- Updated (UTC): 2026-10-17T03:24:30Z
- Branch: `develop`
- Verification/Git ledger commit: `cd4032b2e25940df261b0dfca301429a54fe87ec`
