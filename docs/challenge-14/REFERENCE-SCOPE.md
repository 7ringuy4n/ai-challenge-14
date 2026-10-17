# Requirement and reference scope

## Governing scope

Implement the single Challenge 14 Claims Workflow Orchestrator defined in
`requirements/AI_Challenge_14.md`, `rules/REQUIREMENT.md`, and the current
`CHANGE_REQUEST.md`. Keep the approved Java 21, Spring Boot 3.5.16, MyBatis
3.0.5, PostgreSQL 16, Liquibase, YAML configuration, and one-deployable-service
architecture. The applicable Java module/package rules are in
`rules/JAVA_PROJECT_RULES.md`.

## Exclusions

The change request is a format and process template, not authorization to import
unrelated messaging platforms, separate microservice topologies, alternate
languages, migrations, or business domains. The supplied Java reference's
reusable engineering practices apply within this one claims application; Kafka,
RabbitMQ, gRPC, Redis, JWT, JPA, Flyway, and additional deployable services remain
out of scope under the current requirements.

## Git instruction resolution

The change request asks to update `main` from `origin/main` and derive `develop`
from it. The user's later explicit decision for this implementation is to continue
on `develop`, not reset branches, and not fabricate missing `main`/`origin`
history. The current checkout reports branch `develop`, but has no commit at
`HEAD`, no `refs/heads/main`, no `refs/remotes/origin/main`, and no advertised
remote branch references. Internal `refs/codex/*` objects are not verified
`main` history. The worktree files are untracked. No operation in the current
implementation step requires a valid `main` base; therefore work continues on
`develop` without creating commits, merging, resetting, or pushing. If later work
requires a main-based diff, merge, or synchronization, stop and request the user
to identify/restore a verified base first.

## Verification limits

Java 21 and Maven 3.9.11 are available in the user's local toolchain and a
42-test Maven run passed on the current integrated source hash. Docker CLI exists but
access to `/var/run/docker.sock` is denied. PostgreSQL/Testcontainers, Liquibase
startup, Compose setup/smoke, and runtime verification remain unverified until
Docker access is available. Host-managed model request/response payloads are
unavailable; invocation audit remains `PARTIAL`, with raw payload fields marked
`NOT_CAPTURED`.


The current implementation work does not require a main-based Git operation. Before any later operation that depends on verified `main` history, the user must identify or restore the intended base; no such operation has been attempted.

## 2026-10-17 Git baseline resolution

The user authorized initializing `main` if no baseline exists. Inspection found no
local branch/tag refs or commit objects; `HEAD` is unborn on `develop`. The
configured GitHub `origin` URL is `git@github.com:7ringuy4n/ai-challenge-14.git`.
An authorized `git ls-remote --heads origin` returned exit 0 with no refs. The
repository therefore has no remote branch baseline to use. The `refs/codex/*`
entries point to Git trees only, not commits, and are retained as internal
snapshots rather than treated as branch history. The stale `branch.main` config
has no corresponding ref. A planning/contracts-only initial `main` baseline is
appropriate; application code and in-progress evidence remain on `develop`.

GitHub identity returned login `7ringuy4n`; the matching account noreply address
is used for requested commits. Docker socket inspection shows mode `0660`, owner
`nobody`, group `nogroup`; the process has groups `tringuyen` and `nogroup`, so
socket access is denied. No permissions or group memberships were changed.

### Superseding baseline decision — 2026-10-17

The prior instruction to request clarification before a main-dependent operation is
superseded by PROMPT-20261017-013. After confirming origin advertises no refs, the
user authorized a fresh main baseline. Application implementation must remain on
develop; the new main commit is limited to project rules, requirements, contracts,
API schema, and original plan. A full pre-branch recovery tree was preserved at
`refs/codex/snapshots/pre-main-final-20261017` before the branch switch.
