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

The initial checkout had no commits and `origin` advertised no refs. The user
authorized a planning/contracts-only main baseline. `main` is now at
`521cb1427f910ebbd01129cc1a950c55b899ee87`; `develop` descends directly from
that commit. Application implementation remains exclusively on `develop`.
Detailed operations and the recovery snapshot are recorded in
`reports/verification/git-history.md`.

## Verification limits

Java 21 and Maven 3.9.11 are available locally. The latest clean Maven run passed
120 unit and 9 PostgreSQL/Testcontainers integration tests; JaCoCo measured
97.3799% line and 90.1478% branch coverage. Compose migration/startup, repeat
setup, smoke, and 20 live API scenarios passed when run with approved escalation
because the default sandbox blocks Docker socket and localhost access. No
permissions were changed. Host-managed model request/response payloads remain
unavailable; invocation audit remains `PARTIAL`, with raw payload fields marked
`NOT_CAPTURED`.


This statement describes the state before the later baseline decision below and is superseded by PROMPT-20261017-013.

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


After baseline initialization, `main` is at `521cb1427f910ebbd01129cc1a950c55b899ee87`; `develop` descends directly from it. The source-free main tree contains only project requirements, governance, task plan, and API/design contracts. Current app work stays on develop. Full Git operation detail is in `reports/verification/git-history.md`.

## 2026-10-17 conditional merge resolution (REQ-017)

The latest user instruction authorizes a local `develop` → `main` merge only after
all mandatory acceptance criteria pass, including the full historical
host-managed LLM audit. This supersedes the earlier blanket instruction not to
merge/push in REQ-015, but does not authorize pushing. If full historical capture
is irrecoverable, no merge is allowed; retain the evidence gap and request an
explicit acceptance-policy decision.
