# Challenge 14 — implementation requirements

## Decision

Implement the Claims Workflow Orchestrator in this workspace. Use Java and the
approved coordinator/worker workflow. Implementation, documentation, UI, errors,
tests and progress updates are in English. Keep original human messages verbatim
in their original language in the separate AI conversation log.

## Delivery requirements

1. Record requirements before implementation and maintain this file when scope changes.
2. Use Java 21, Spring Boot, Spring Security, Maven, PostgreSQL, MyBatis, Liquibase,
   YAML application configuration with environment variables, Docker/Compose,
   JUnit 5, Mockito, AssertJ, Testcontainers and JaCoCo. Pin compatible versions. Keep
   configuration hierarchy aligned with application ownership: `claims.workflow`,
   `claims.security`, and `claims.outbox`; retain Spring-managed datasource and
   Liquibase settings under `spring.datasource` and `spring.liquibase`.
3. Implement the eight states and nine configured transitions in
   `requirements/AI_Challenge_14.md`, including preconditions, role authorization,
   mock side effects after successful transitions and specific failure reasons.
4. Make state/transition definitions configurable in YAML. Demonstrate adding a
   state and transition by a configuration-only diff using existing primitives.
5. Provide an immutable per-claim audit trail with timestamp, from/to, actor ID,
   role and notes. Persist state, audit and effect requests atomically.
6. Allow three information-request cycles; reject the fourth attempt with exactly
   “Maximum information requests exceeded — escalate to team lead”.
7. Provide a web interface showing state, available transitions with required
   roles, transition controls and audit history. Enforce authorization server-side.
8. Implement and run the five specified scenarios, a precondition failure and
   cycle-limit evidence. Include at least 15 meaningful tests, plus persistence,
   concurrency, security and failure tests appropriate to the implementation.
9. Apply meaningful State Machine, Strategy, Factory, Repository and SOLID
   boundaries. Use safe BusinessException handling, structured trace/span logging,
   optimistic concurrency, idempotency and after-commit mock dispatch.
10. Verify actual results. Print test progress as `running test case N/TOTAL`.
    Fix core causes and rerun affected checks. Never fabricate execution, coverage,
    deployment or performance results; record unavailable checks as NOT_RUN.
11. Maintain `history/task_on_progress.md`, `history/CHANGELOG.md`, dated technical
    history entries, `docs/HISTORY.md` and `docs/CHANGELOG.md` after updates.
    `.agent-memory/task_on_progress.md` is the authoritative recovery state;
    the history progress file is a synchronized human-readable view.
12. Keep portable shared memory and checkpoint before and after implementation
    steps. For a new project, do not require or invent a previous completed task
    to archive; initialize progress from the current request.
13. Keep a **separate AI conversation log** containing the **verbatim human
    messages sent to the coding agent**, including this request and subsequent
    changes. Include worker task packets/reviews separately. Submission without
    the human AI conversation log is an **automatic failure** under the challenge
    README. Do not replace the raw log with a summary or invented conversation.
14. Include setup/run instructions, workflow YAML, scenario outputs, audit output,
    a configuration-extension diff, test evidence, architecture decisions and an
    honest timeline estimate. Prepare a repository-ready submission locally.
15. Use the existing model routing: coordinator `gpt-6.1-sol` medium; implementation
    and independent-review subagents `gpt-6-luna` medium. Workers draft isolated
    patches; coordinator integrates and runs checks. Respect the host's actual
    model capabilities and filesystem permissions.
16. Store an importable Postman collection and secret-free environment template
    under `postman/`. Document setup, account/role switching, CSRF refresh, expected
    transitions, idempotency keys, pagination/audit reads and access denial probes
    in `postman/README.md`. Never store real credentials, cookies or CSRF tokens in
    the exported collection or environment.
17. Organize Java source by feature using
    `<base-package>/<feature>/{api,api/dto,service,domain}`. Place feature
    controllers and transport DTOs under `api/`, use-case contracts and
    implementations under `service/`, and business values, repository ports,
    mapper interfaces, and typed persistence projections under `domain/` (with
    focused `domain/mapper/` and `domain/persistence/` subpackages where useful).
    Place cross-feature HTTP errors/advice in `common/web`, framework beans in
    `config`, identity/security adapters in `security`, shared audit code in
    `audit`, tracing in `observability`, and workflow interpretation in
    `workflow/`. Mirror production ownership in test packages. Keep routes,
    request/response JSON, authorization, transaction, persistence, error and
    ordering behavior unchanged by the package refactor. Use the generic
    boundaries in `rules/JAVA_PROJECT_RULES.md` for this and future Java modules.


18. Store Liquibase master changelog at
    `src/main/resources/db/changelog/db.changelog-master.yml`; place included
    YAML/SQL changesets under `src/main/resources/db/changelog/changes/`. Set
    `spring.liquibase.change-log` to that exact classpath location and let Maven
    package the standard resource tree without a root-directory remap. Keep mapper
    XML under `src/main/resources/sql/service/<feature>/`, matching MyBatis
    `mapper-locations`. Document and test each classpath path used by production
    and Testcontainers.
19. Classify review artifacts under `reports/java-performance/` for JMeter plans
    and measured capacity runs, `reports/sub-agents/` for worker patches/handoffs
    and independent reviews, and `reports/verification/` for build/test/runtime
    evidence. Ignore generated worker drafts and verification/load-run output in
    Git; retain only intentional plans, source documentation and hand-maintained
    acceptance material. Do not produce a distribution bundle unless explicitly requested.

20. **Mandatory requirement capture for every new user message.** Before planning,
    reviewing, modifying code, or dispatching an agent, append each new user request,
    clarification, correction, follow-up prompt, and change of scope to
    `docs/submission/user-prompts.jsonl` in its original language, preserving the
    verbatim content and original order. Also update `rules/REQUIREMENT.md` for
    every such message: add a dated, sequential requirement/change entry with a
    unique ID, source prompt record ID, scope, acceptance criteria, status and
    supersession links. Record even requests that are rejected, postponed,
    ambiguous or do not ultimately change code; explain the disposition rather
    than silently omitting them. Never overwrite previous entries or fabricate
    missing prompt text. Log a new entry for each message rather than replacing
    the previous requirement. Do not mistake an agent's peer-review feedback for
    a human instruction: record reviews separately and register any resulting
    requirement change with its actual source.
21. **Mandatory LLM request/response trace while implementing.** For every model
    interaction involving the coordinator, coding workers, independent peer
    reviewers, retries and follow-up calls, capture the actual outbound request
    AND corresponding inbound response verbatim whenever those payloads are exposed
    by the invoking runtime. For inaccessible host-managed calls, capture available
    dispatch instructions, outputs and metadata and mark raw fields NOT_CAPTURED
    under requirement 23 in an append-only AI interaction
    log (`docs/submission/llm-interactions.jsonl` plus referenced raw payload
    files when necessary). Capture system/developer instructions and context
    supplied to the model when accessible, prompt/messages, tool-call requests,
    tool outputs where accessible, and the model's returned text/tool calls.
    Record each invocation before sending and update its outcome immediately on
    return, failure, interruption or timeout; never reconstruct a transcript
    from memory. Preserve partial/streamed output where available.
22. Each LLM invocation must be traceable by an immutable interaction ID and
    linked requirement ID(s), task/attempt ID, agent role, selected and actual
    model/provider (if known), timestamp with timezone, request/response IDs,
    invocation status, retry/parent ID, and paths or hashes of raw evidence.
    Record token usage, latency, and error details only when actually available;
    explicitly label unknown or unavailable fields. Keep original ordering so
    a reviewer can reconstruct **human prompt -> requirement revision -> agent
    request -> model response -> code patch -> independent review -> tests ->
    acceptance**. Never claim that a missing or inaccessible host-level model
    request/response has been logged. Mark it `NOT_CAPTURED`, provide reason,
    and block any claim of complete end-to-end traceability for that run.
23. Preserve raw evidence without exposing secrets: redact credentials, API keys,
    session cookies, sensitive claim contents and personal data before committing
    or sharing logs; record a redaction marker, field/reason and digest or
    reference where allowed. Do not log hidden provider reasoning or private
    internal state that the host does not expose. Restrict access to raw logs,
    keep them outside public distribution when sensitive, and document retention.
    Do not let a telemetry failure silently result in untraceable model calls:
    fail closed before dispatch for application-controlled or directly managed
    LLM API calls when actual outbound request logging cannot be established;
    explicitly flag failures to persist inbound responses. Host-managed coding
    agent calls that do not expose raw model payloads may proceed provided all
    available task instructions, outputs, invocation metadata and review artifacts
    are preserved; mark inaccessible payloads `NOT_CAPTURED` with the reason and
    affected interaction ID. Such runs carry `PARTIAL` audit status and MUST NOT
    claim full end-to-end traceability. A `PARTIAL` audit alone does not block
    implementation, tests or independent review; do not downgrade any other
    security, quality or permission gate. When the request explicitly requires
    fully captured host-managed payloads as an acceptance condition, report it
    unmet rather than claiming completion.

## Requirement change register

| ID | Date (Asia/Ho_Chi_Minh) | Source | Requirement change | Status |
| --- | --- | --- | --- | --- |
| REQ-20261017-001 | 2026-10-17 | Current user message (see verbatim prompt log) | Record every new user prompt in this file, including new follow-ups, changes and peer-review-derived decisions; capture every actual LLM request/response during implementation for later tracing. | Rule added; instrumentation not yet verified |
| REQ-20261017-011 | 2026-10-17 | Implementation decision derived from REQ-20261017-010 and AD-01 | Complete the existing member rejection-acknowledgment evidence path as an explicit owner-only, idempotent use case, and expose configured available transitions for the client. This adds no states, edges, roles, guards, or effects. | In progress; AD-02 recorded; implementation and review pending |

## Scope interpretation

The supplied change-request example is a **format reference**. Unrelated Hermes,
Zalo, Telegram, OmniRoute, Go migration and VPS operations in that example do not
become tasks for this claims application. Read applicable engineering rules and
adapt them to this project. Keep external reference paths/details out of project
history and public implementation documentation; preserve raw messages unchanged.

The Java reference describes three other systems. Apply relevant Java engineering
standards to one modular claims application. Use Liquibase as the migration tool;
Kafka/RabbitMQ/gRPC, three microservice platforms and comparative benchmarks are
outside this challenge's implementation scope unless separately requested.

## Current environment findings

Java21 and Maven3.9.11 are available through the local toolchain. Docker CLI and
Compose are installed; authorized Docker access runs real PostgreSQL Testcontainers
checks. Normal sandbox networking cannot resolve dependency repositories; authorized
dependency downloads use the local ignored Maven cache. Final runtime and coverage
gates remain required.

## Acceptance tracking

Track implementation in `rules/workflow/tasks.json`, a project plan created from
the current requirements, and the synchronized progress/history files. Task acceptance requires all required
checks and independent review. Design/validator tests do not count as Java
challenge tests. Final documentation must describe the actual implemented system.


## Java reference gap-audit addendum

Apply reusable backend conventions from the Java reference to this single modular
claims application. The three sample platforms' deployment topology and messaging
choices do not apply. Keep the frozen Challenge 14 stack (Java 21, Spring Boot
3.5.16, MyBatis 3.0.5, PostgreSQL 16 and Liquibase) and current authentication
contract. Do not add Kafka, RabbitMQ, gRPC, Redis, JWT, Flyway, JPA, separate
services, comparison benchmarks or unrelated business domains.

The audit found these applicable gaps to close:

1. Replace literal HTTP path fragments in controller mappings with named constants
   for API and resource bases. Preserve all existing paths and authorization.
2. Bound collection reads with `page` (zero-based, default 0) and `size` (default
   20, maximum 100), a typed response containing `items`, `total`, `page`, `size`
   and `totalPages`, count plus page SQL, and deterministic ordering. Apply this to
   visible-claim and audit-history lists; keep member ownership filtering in SQL.
   Update the browser client and OpenAPI contract in the same change.
3. Add an inbound `X-Correlation-ID` contract: accept only bounded safe ASCII values,
   otherwise generate a UUID; return the effective ID in the response header and
   structured logs/MDC. Keep correlation ID distinct from Micrometer trace/span IDs.
4. Add Micrometer Prometheus registry and expose `/actuator/prometheus` behind the
   existing authenticated actuator policy; keep public health details hidden.
5. Update architecture, API, operations and test documentation to describe the
   implemented pagination, correlation and metrics contracts. Preserve the current
   error envelope and the one-deployable-service architecture.

Acceptance requires regression coverage for route preservation, page boundaries,
count/read consistency, member isolation, stable audit ordering, malformed or
oversized correlation IDs, response/log context, and actuator authentication. Run
focused tests and the full Java quality gate; update actual test totals, coverage,
OpenAPI, history and the local submission archive only after verification.

## Architecture, audit and capacity addendum

1. Add a runtime @Auditable(action, resource) method annotation and Spring aspect
   for selected claim writes. Persist supplementary system-operation records in a
   dedicated append-only PostgreSQL table. Keep claim_audit unchanged as the
   domain transition history. Include action/resource/resource ID, authenticated
   actor, correlation ID, outcome, safe error type and timestamp. Do not serialize
   method arguments by default; never record credentials, tokens, claim documents,
   or unrestricted fact payloads. Successful audit insertion participates in the
   same transaction as the write, so an unaudited successful write cannot commit.
   A rejected operation may receive a separately committed failure record without
   replacing the original business error.
2. Idempotency applies to every mutating claim operation (create, facts update,
   human transition and system transition). Keys are scoped by authenticated actor
   and operation/resource; a matching key and request fingerprint returns the
   original result without repeating domain or system audit rows. Reusing a key
   with a different request returns a stable conflict. Concurrent identical
   requests produce one durable mutation and one effective audit event. Preserve
   the existing transition replay behavior.
3. Keep this as one modular Java/PostgreSQL service. Do not add Kafka, RabbitMQ,
   gRPC, external RPC or another broker. The existing database outbox remains the
   local durable mechanism for mock Challenge 14 effects.
4. Create architecture and workflow diagrams as image files under
   docs/architecture/images/, with editable source beside each exported image.
   Diagrams must match the implemented system and include the audit/idempotency
   path, PostgreSQL transaction boundary, local outbox, and configured claim states.
5. Add a generic capacity profile for 100 and 500 concurrent HTTP clients. Include
   reproducible JMeter plans, the required Compose setup/smoke sequence, dataset
   preparation, duration/ramp/think-time settings, error/latency/throughput
   measurements, resource monitoring, and report locations. Provide a clearly
   labeled starting hardware profile for each target; it is a planning estimate,
   not a measured service guarantee.
6. Keep JaCoCo gates at least 95% line and 90% branch with no business-code
   exclusions. Add meaningful tests for new audit/idempotency paths and rerun the
   gates until the added code satisfies them. Run Compose smoke after setup and
   execute the 100/500-client JMeter profiles only on an explicitly prepared test
   environment. Record actual test, coverage, smoke and load results; label runs
   that cannot be performed NOT_RUN with the exact command and reason.
7. Update OpenAPI, architecture/security/testing/operations docs, history,
   changelogs, requirement and delivery manifest. Do not claim capacity, SLA or
   performance until the corresponding load plan has actually run.

## REQ-20261017-002 — 2026-10-17

- Source: PROMPT-20261017-002, verbatim implementation request.
- Scope: implement CHANGE_REQUEST.md and requirements/AI_Challenge_14.md under existing engineering, tracing and independent review gates.
- Acceptance: working Java application; verified scenarios, security, persistence and coverage; independent review; complete request/response evidence or explicit gaps without acceptance.
- Status: superseded by REQ-20261017-003 for project initialization scope; the no-dispatch trace gap remains recorded in `docs/submission/llm-interactions.jsonl`.
- Disposition: this is a fresh project initialization. Missing source, build, application documentation, and prior history are expected and must be created from the requirements. Existing task manifest status and draft artifacts are unverified and must not be treated as accepted implementation evidence.
- Remaining gate: establish orchestration-boundary request/response capture before model dispatch, as required by requirements 21–23.

## REQ-20261017-003 — 2026-10-17

- Source: PROMPT-20261017-003, verbatim prompt log.
- Scope: initialize this as a new project; remove stale documentation and workflow assumptions that required prior project files, completed-task archives, existing application/runtime logs, or recovered source as prerequisites.
- Acceptance: startup instructions match the current blank application checkout; retain requirements and acceptance criteria for creating the application; distinguish absent optional historical artifacts from real blockers; no fabricated source, history, or verification claims.
- Status: in progress.
- Supersedes: REQ-20261017-002's recovery assumptions about missing application/build files, archived progress, historical artifacts, and existing worker drafts. Mandatory request/response tracing and review gates from REQ-20261017-001 remain in force.

## REQ-20261017-004 — 2026-10-17

- Source: PROMPT-20261017-004, verbatim prompt log.
- Scope: implement the current `CHANGE_REQUEST.md` in this project.
- Acceptance: implement its requested changes, satisfy applicable focused and full quality gates, obtain independent review, and record truthful verification and submission evidence.
- Status: blocked before implementation dispatch.
- Supersedes: none.
- Disposition: required host-level LLM request/response capture is not exposed in this execution, so the workflow fail-closed rule prohibits dispatching worker/reviewer model calls; no independent-review or completion claim is possible.

## REQ-20261017-005 — 2026-10-17

- Source: PROMPT-20261017-005, verbatim prompt log.
- Scope: implement `/home/tringuyen/Documents/ai-challenge-14/CHANGE_REQUEST.md` in this workspace.
- Acceptance: satisfy the request's implementation and project quality gates, perform independent review, and record truthful verification and submission evidence.
- Status: in progress.
- Supersedes: none.
- Disposition: new-project initialization is permitted by `rules/workflow/START.md`; missing optional memory and historical files are to be initialized. Host-managed invocation raw payloads will be marked `NOT_CAPTURED` if unavailable, with exposed task packets and outputs retained.

## REQ-20261017-006 — 2026-10-17

- Source: PROMPT-20261017-006, verbatim prompt log.
- Scope: approve the proposed Spring Boot/PostgreSQL design and proceed autonomously through design spec, implementation plan, application implementation, testing, independent review and final reporting for REQ-20261017-005.
- Acceptance: runnable complete application; dependency-ordered implementation; transaction, idempotency, concurrency and constraints; truthful interaction logging with inaccessible host payloads marked `NOT_CAPTURED`; meaningful tests with progress, independent review, synchronized project records and reproducible startup.
- Status: in progress.
- Supersedes: none. This approval satisfies the design approval gate and expressly waives further phase approval requests for this work.
- Disposition: preserve all existing requirements and acceptance criteria; proceed without further phase approval absent a genuine essential-requirement, security or irreversible-decision blocker.


## REQ-20261017-007 — 2026-10-17

- Source: PROMPT-20261017-007 in `docs/submission/user-prompts.jsonl`.
- Scope: implement the current `CHANGE_REQUEST.md` in this workspace.
- Acceptance: satisfy implementation, focused/full verification, independent review, and truthful evidence requirements in the change request and governing workflow.
- Status: in progress.
- Supersedes: none.
- Disposition: follow coordinator-only source edits, task dependency gates, and host-managed interaction logging; missing raw host payloads are marked `NOT_CAPTURED` and audit status remains `PARTIAL`.

## Execution-host correction — 2026-10-17

The initial environment note above described a previously observed toolchain, but the current execution host differs: `command -v java`, `command -v javac`, and `command -v mvn` returned no executable; `mvn -B test` exited 127. An attempted elevated `apt-get update && apt-get install -y openjdk-21-jdk-headless maven` exited 100 because the process cannot access the apt lock directory. Docker Compose is installed, but `docker version` cannot connect to `/var/run/docker.sock` (permission denied). Therefore T00 remains blocked and no dependent worker/reviewer dispatch is allowed until the prerequisite build check can pass.


## REQ-20261017-008 — 2026-10-17

- Source: PROMPT-20261017-008 in `docs/submission/user-prompts.jsonl`.
- Scope: continue the approved application implementation despite unavailable host runtimes; investigate non-root build alternatives and implement the full service on `develop`.
- Acceptance: advance implementation independently of blocked runtime checks; preserve strict verification status, independent static review, Git/LLM evidence, and do not claim verified/production-ready until compile, tests, migrations and startup pass.
- Status: in progress.
- Supersedes: prior interpretation that T00's unavailable build check prohibited all dependent implementation work. T00 runtime evidence remains blocked and acceptance gates remain unchanged.
- Disposition: proceed with coordinator-owned implementation while runtime verification is unavailable; do not dispatch dependency-gated workers unless their prerequisites are accepted.


## Execution-host follow-up — 2026-10-17

A broader non-root search found Java 21.0.12.1 at `/home/tringuyen/.local/java/jdk-21` and Maven 3.9.11 at `/home/tringuyen/.local/maven`; the earlier `command -v` probe was PATH-only and missed these installations. The normal `.m2` cache is read-only, so a writable copy at `/tmp/claims-m2` was used. Sandboxed DNS could not resolve Maven Central; the configured dependency-download escalation succeeded. `mvn -B test` then passed with 8 tests. Docker is still unavailable: user `tringuyen` is not a member of the socket's `nobody:nogroup` groups, and `/var/run/docker.sock` access is denied. No permission changes were attempted.


## REQ-20261017-009 — 2026-10-17

- Source: PROMPT-20261017-009 in `docs/submission/user-prompts.jsonl`.
- Scope: continue implementing the current `/home/tringuyen/Documents/ai-challenge-14/CHANGE_REQUEST.md` on `develop`.
- Acceptance: complete as much implementation as possible; preserve coordinator-only integration, independent review, actual command evidence, and environment blockers without claiming unverified runtime success.
- Status: in progress.
- Supersedes: none.
- Disposition: continue from the authoritative progress checkpoint and keep database/Docker-dependent gates separately blocked when they cannot run.


## REQ-20261017-010 — 2026-10-17

- Source: PROMPT-20261017-010 in `docs/submission/user-prompts.jsonl`.
- Scope: continue implementing the current `/home/tringuyen/Documents/ai-challenge-14/CHANGE_REQUEST.md` on `develop`.
- Acceptance: satisfy implementation requirements as far as possible, preserve independent review and actual command evidence, and keep unavailable environment checks distinct from implementation progress.
- Status: in progress.
- Supersedes: none.
- Disposition: resume from the authoritative checkpoint; do not fabricate missing `main`/origin history or treat draft worker handoffs as accepted source.

## REQ-20261017-012 — 2026-10-17

- Source: PROMPT-20261017-012 in `docs/submission/user-prompts.jsonl`.
- Scope: implement `/home/tringuyen/Documents/ai-challenge-14/CHANGE_REQUEST.md`.
- Acceptance: complete the authorized implementation and synchronize evidence, documentation, and task state; do not claim unavailable verification.
- Status: in progress.
- Supersedes: none.
- Disposition: continuation of the active implementation with the environment fallback and Git constraints from the preceding user decision. Independent review-17 feedback is linked in `docs/submission/llm-interactions.jsonl` (LLM-20261017-013-T02-REVIEW-17): proxy advice binding caveat closed; persistence and `REQUIRES_NEW` remain unverified with a fake repository.

## REQ-20261017-013 — 2026-10-17

- Source: PROMPT-20261017-013 in `docs/submission/user-prompts.jsonl`.
- Scope: establish a safe verified or new Git baseline, preserve and continue implementation on `develop`, improve meaningful coverage, execute available integration/runtime checks, and maintain honest acceptance status.
- Acceptance: inspect local and remote refs; never discard work or force-push; do not put unfinished implementation on `main`; commit reviewed logical changes to `develop`; pass required coverage, tests, review, migrations, startup and scenario gates before acceptance.
- Status: in progress.
- Supersedes: none.
- Disposition: remote state must be distinguished from a failed network/auth request; if the repository has no existing history, establish a minimal planning/contract baseline on `main`, then continue implementation from it on `develop` while preserving all current files on a recoverable snapshot ref.


## REQ-20261017-014 — 2026-10-17

- Source: PROMPT-20261017-014 in `docs/submission/user-prompts.jsonl`.
- Scope: resolve Git baseline safely and continue claims workflow implementation, meaningful coverage, PostgreSQL verification, independent review, and synchronized evidence.
- Acceptance: implementation remains on `develop`; do not discard work or merge unfinished code into `main`; meet 95% line / 90% branch coverage, complete database/startup/scenario verification, and independent review before any acceptance.
- Status: in progress.
- Supersedes: none.
- Disposition: the empty remote was preserved as evidence; planning/contracts-only main baseline `521cb1427f910ebbd01129cc1a950c55b899ee87` was initialized and `develop` continues from it. Reviewed implementation commit `207c70d7e559af9be86fb5cf9e9b538a42b8bf22` is on `develop`. The final clean quality run passed 120 unit and 9 PostgreSQL/Testcontainers tests, applied all five Liquibase changesets, and measured 97.3799% line / 90.1478% branch coverage. Repeat Compose startup, authenticated smoke, and the 20-case live API workflow passed. Independent review found no remaining critical/high finding after the coordinator fixed and rereviewed a CAS test race. Preserve review raw payload limitations as `NOT_CAPTURED` / `PARTIAL`; JMeter 100/500-client runs and a single end-to-end run of the entire 29-case matrix are still `NOT_RUN`. Work remains in progress and is not accepted or complete.

## REQ-20261017-015 — Remaining acceptance gates (2026-10-17)
- Source: verbatim human prompt in `docs/submission/user-prompts.jsonl`.
- Requirement: execute all 29 PostgreSQL-backed live scenarios in one combined run with request/response evidence; provide reproducible Apache JMeter profiles and actual performance metrics against approved targets; implement and evidence OpenTelemetry propagation across HTTP and asynchronous outbox with log correlation; update requirement traceability; reconcile actual and inaccessible LLM records; verify a no-manual-edit Postman workflow; verify editable/rendered architecture, workflow, database, security, and deployment diagrams; retain 129-test and JaCoCo gates; independently review changes; synchronize project history, reports and task evidence; commit verified changes to `develop` without merge or push.
- Constraints: no fabricated evidence, actual commands/timestamps/environment; distinguish PASS, FAIL, BLOCKED and NOT_RUN. Ask for approval when mandatory performance thresholds are undefined. Do not accept until all gates pass.
- Status: in progress; acceptance explicitly withheld pending evidence and thresholds.
- Supersedes: none.

## REQ-20261017-016 — Performance threshold clarification (2026-10-17)
- Source: verbatim response in `docs/submission/user-prompts.jsonl`.
- Requirement: compare measured load runs against P95 latency ≤ 500 ms, P99 latency ≤ 1,000 ms, and HTTP error rate ≤ 1%.
- Status: approved by the user; resource saturation/database/outbox metrics must still be reported separately as observed measurements.
- Supersedes: no prior numeric threshold; resolves REQ-20261017-015's missing latency/error targets.

## REQ-20261017-015/016 — Verification outcome (2026-10-17)
- Status: implementation and specified evidence substantially complete; overall acceptance remains NOT_ACCEPTED.
- PASS evidence: combined live API 29/29 against PostgreSQL; clean Maven 123 unit + 9 PostgreSQL/Testcontainers tests, six migrations, 97.0464% line / 90.1116% branch; seven JMeter profiles within approved latency/error targets; Newman 27 requests/14 assertions; actual OTLP HTTP→security→outbox child-span/log correlation evidence; editable sources and SVG/PNG for five diagram families; independent review has no critical/high finding.
- Provenance qualification: seven full JMeter runs were against the business/outbox/tracing implementation before a final HTTP trace-context log field adjustment. The post-adjustment five-second baseline verifies only runner metadata/compression, not full load performance. A malformed-metadata diagnostic run is retained and excluded from comparison.
- Remaining: browser UI scenario, live cross-member scenario with two runtime principals, requirement-level HTTP execution of all individual criteria (the 29 cases are grouped), byte-identical full JMeter rerun, and full raw host-managed LLM payload capture. LLM audit remains PARTIAL with inaccessible records NOT_CAPTURED.
- Authoritative current evidence: `reports/verification/REQ-015-final.json`; trace matrix: `docs/testing/traceability-matrix.md`; JMeter aggregation: `reports/performance/comparison.json`.


## REQ-20261017-017 — Mandatory completion and final merge (2026-10-17)

- Source: PROMPT-20261017-017 in `docs/submission/user-prompts.jsonl`; the full prompt is preserved verbatim from the attached file and its SHA-256 is recorded in that entry.
- Scope: complete the final JMeter byte-identical reruns, real browser UI verification, live cross-member isolation, requirement-by-requirement verification, and expanded local Codex/LLM audit; update reports; merge `develop` into `main` only if every mandatory gate passes. Never push.
- Acceptance: all prescribed quality, live, performance, security, browser, traceability, documentation and audit criteria have real evidence; the final acceptance matrix has no unresolved FAIL/BLOCKED/NOT_RUN gates. Full host-managed capture is expressly mandatory; if inaccessible historical payloads cannot be recovered, disclose the irrecoverable gap, do not merge, and request an acceptance-policy decision.
- Status: in progress; no merge authorized until acceptance conditions are genuinely met.
- Execution order: inspect repository and evidence; fingerprint exact JMX/config and source; execute seven profiles; execute browser and live ownership workflows; map each requirement to executable or documentary verification; inventory accessible session and archive evidence; then regression tests, independent review, and final report.
- Ownership: coordinator owns all source/config/shared requirements/manifests/reports. Verification workstreams T13–T16 have disjoint report artifacts and read-only boundaries; production source changes, if required, remain serialized under coordinator ownership.
- Supersedes: no prior requirement; tightens completion/merge policy beyond REQ-015/016.
- Disposition: preserve current `main` baseline and `develop` commits. An explicit historical host-managed payload condition remains binding; no downgrade or inferred capture.

## REQ-20261017-018 — Approved member principal configuration (2026-10-17)

- Source: PROMPT-20261017-018 verbatim approval in `docs/submission/user-prompts.jsonl`.
- Decision: implement the approved design: a validated configurable `CLAIMS_MEMBER_USERNAMES` list, defaulting to `member-demo`; all configured local members retain the existing human password and MEMBER role. This enables two identities for live PostgreSQL ownership verification without changing the default member account.
- Acceptance: names must be valid, unique member principals; no built-in staff/system identities can be shadowed; default profile remains compatible. Tests assert configuration validation and both principals' assigned roles. Live test proves cross-member GET/list/update/transition denial and unchanged database state.
- Status: approved and in progress; requirement is specific to local/demo in-memory authentication, not a persistent identity-provider migration.
- Parent: REQ-20261017-017 / T14. Implementation ownership: `src/main/java/com/papaya/claims/security/SecurityConfiguration.java`, `src/main/resources/application.yml`, `docker/compose.yml`, `src/test/java/com/papaya/claims/security/SecurityConfigurationTest.java`; browser scenario source and evidence paths are separately listed on T14.


## REQ-20261017-019 — Approval confirmation recorded (2026-10-17)

- Source: PROMPT-20261017-019 in `docs/submission/user-prompts.jsonl`.
- Decision: user confirmed approval of the REQ-018 design; the validated local member identity list keeps the `member-demo` default and uses the configured human password with MEMBER role.
- Disposition: continue the already authorized REQ-018 implementation and live tests; this confirmation does not change the scope or production identity-provider behavior.
- Status: implemented; verification in progress under T14.

## REQ-20261017-017 — Verification disposition (2026-10-17)

- Source: the attached verbatim PROMPT-20261017-017 record; this entry records execution outcome and does not replace or waive the submitted requirements.
- Final-source performance: all seven approved JMeter profiles actually executed against clean source commit `0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2`, with unchanged JMX and profile properties hashes. Six passed. Stress failed the approved P95 limit (564 ms vs 500 ms; P99 767 ms; errors 0%). A later default run failed P95 917 ms/P99 1,077 ms. Diagnostic connection-pool and Tomcat variants were worse; independent review recommends no tuning based on sequential, changing database state. The gate is FAIL; targets were not weakened.
- Functional and quality evidence: 127 unit + 9 PostgreSQL/Testcontainers tests, six migrations, 97.0195% line and 90.2326% branch coverage, 29 combined live cases, 17 real Chrome cases, 7 live cross-member checks, Postman/Newman and trace propagation passed; evidence is linked in `reports/acceptance/FINAL-ACCEPTANCE.md`.
- Local model session audit: refreshed export covers 64 project-related sessions and 8,172 accessible records; transcript hashes and contained unique paths pass. Host-managed raw request/response payloads, IDs, system/developer envelopes and encrypted reasoning remain `NOT_CAPTURED`; the full-capture requirement remains BLOCKED and is not waived.
- Status: IN_PROGRESS_NOT_ACCEPTED. No merge or push. Final-source JMeter run evidence is in `reports/verification/REQ-017-jmeter-final/`; the reviewed diagnostic experiments are in `reports/verification/REQ-017-jmeter-ab/`; full gate matrix is `reports/verification/REQ-017-requirement-matrix.json`.
- Required next intervention: a dedicated performance environment for controlled same-database stress profiling, plus a supported historical export for host-managed model payloads or an explicit user acceptance-policy decision. Until these mandatory conditions are resolved, `develop` must not merge to `main`.


### 2026-10-17 REQ-017 audit inventory refresh

After the T15 rereview request, the local session exporter was rerun. It found 65 project-related sessions among 145 candidates and exported 8,310 accessible message/tool records; all 65 transcript hashes match and paths are unique/contained. Host-managed raw model envelopes, IDs, system/developer payloads and encrypted reasoning remain NOT_CAPTURED; audit remains PARTIAL/BLOCKED.
### 2026-10-17 REQ-017 reporting and audit closeout

T15's reporting task was independently accepted on rereview 04 after correcting the same-run live evidence references, clarifying current application-log trace correlation versus earlier collector-exported span proof, refreshing the requirement-matrix timestamp, and reconciling counts. Latest local export contains 65 project-related sessions and 8,483 accessible message/tool records; 65 transcript hashes and unique contained paths verify. The interaction ledger has 101 entries and 60 interaction IDs. Host-managed raw model payloads remain NOT_CAPTURED and mandatory full-capture status remains BLOCKED. Project acceptance remains NOT_ACCEPTED because the approved stress P95 limit fails (564 ms >500 ms) and the host payload gate is blocked. No merge or push.
## REQ-20261017-020 — Revised acceptance policy (2026-10-17)

- Source: PROMPT-20261017-020 in `docs/submission/user-prompts.jsonl`.
- Scope: Apply the owner's revised hardware-constrained JMeter exception policy and LLM proof-of-work acceptance policy; reassess final acceptance and merge only if every revised mandatory gate passes.
- Acceptance: preserve all original benchmark results and prior FAIL/BLOCKED decisions; establish hardware/test-environment limits with actual resource evidence before assigning `HARDWARE_CONSTRAINED`; do not claim the 500 ms target passed or production-scale performance verified. Verify transcript/session paths, IDs, SHA-256, accessible conversation/tool evidence, and traceability; report host-managed raw payloads `NOT_CAPTURED` and raw completeness `PARTIAL`, while accepting proof-of-work if evidence demonstrates activity. Rerun applicable regressions and workflow validation, obtain independent review, update requirement/history/changelog/Git evidence, and merge `develop` into existing `main` only if all revised mandatory gates pass. Never push.
- Status: accepted with local performance exception after independent review-03 PASS; merged locally into existing `main` as 18f97aa9c8296fdc107a02192aa4e8e62dd45d78; no push.
- Supersedes: only the prior requirement for full raw-payload completeness as an acceptance gate is revised to proof-of-work; inaccessible payloads remain honestly `NOT_CAPTURED`. Prior observed JMeter stress FAIL remains immutable history; REQ-020 evidence-based local exception classification is accepted after independent review.
- Disposition: measured shared-host CPU saturation supports `HARDWARE_CONSTRAINED` under the revised local policy; original P95 failure (564 ms) remains unchanged and production performance remains unverified. Accessible transcript proof-of-work passes with a disclosed active-source growth qualification; raw host payload completeness remains PARTIAL/NOT_CAPTURED. A logged 127+9 Maven regression and 18-task workflow validation pass. Final acceptance is granted as `ACCEPTED_WITH_PERFORMANCE_EXCEPTION` after independent review-03 PASS. The project is `ACCEPTED_WITH_PERFORMANCE_EXCEPTION`; `develop` merged locally into existing `main` at 18f97aa9c8296fdc107a02192aa4e8e62dd45d78; no push.


## REQ-20261017-021 — Synchronize accepted history and verify repository (2026-10-17)

- Source: PROMPT-20261017-021 verbatim in `docs/submission/user-prompts.jsonl`.
- Request: synchronize `develop` and `main` with the accepted application history and verify repository state at `https://github.com/7ringuy4n/ai-challenge-14`.
- Safety: preserve all local and remote history; inspect remote refs before attempting updates; never force-push or discard the untracked `codex-logs.zip`. If remote inspection cannot establish safe ancestry, leave remote untouched and report the exact blocker.
- Acceptance: both local branch heads contain the accepted merge and closeout; the remote repository refs and resulting commits are checked when network/authentication permits; workflow validation and worktree status are recorded.
- Scope: Git and evidence synchronization only; no application source behavior change.
- Status: in progress.
