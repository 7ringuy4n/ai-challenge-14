# Project engineering rules

For Java projects and modules, apply the feature-oriented package blueprint,
layer boundaries, API/security and persistence conventions, resource/configuration
layout, and completion checklist in
[`JAVA_PROJECT_RULES.md`](./JAVA_PROJECT_RULES.md), alongside these
workspace-wide engineering rules. Preserve the target project's stack and public
behavior.

Read rules/REQUIREMENT.md, AGENTS.md, current docs/HISTORY.md and docs/CHANGELOG.md,
and authoritative memory progress before a new requirement. Preserve the human
request verbatim before implementation; keep all generated project content English.

1. Prioritize correctness, security, architectural integrity and durable root-cause fixes.
2. Maintain a concrete checklist/dependency graph before edits. Shared files and
   source integration have one coordinator owner; workers use isolated patch artifacts.
3. Keep engine rules/config separate from HTTP, transactions and database adapters.
   Use meaningful patterns and small interfaces; justify operational complexity.
4. Use trusted authenticated actors, strict validation, exact money types and safe errors.
   Keep secrets out of source/logs/images and use separate database runtime/migration roles.
5. Persist claim/audit/receipt/payment/effects atomically and protect races/idempotency.
   Append audit only; enforce immutability at the runtime database boundary.
6. Reproduce failures, fix core source/setup, add regression variants and rerun affected
   checks. Avoid hotpatch-only repairs, fake success, test weakening and disabled guards.
7. Test actual PostgreSQL grants/SQL/rollback/concurrency when that integration is available.
   Mocks support unit isolation; they cannot replace real integration evidence.
8. First setup provisions and validates readiness only. Explicit test commands follow;
   setup must be safe to repeat and preserve existing project data.
9. Print running test case N/TOTAL; capture command, source hash, exit status and real
   results. Read reports and independently review before accepting. NOT_RUN is not PASS.
10. Inspect relevant project logs after runtime/setup tests for errors/crash loops.
    Unrelated applications, containers and remote hosts are outside the task boundary.
11. Synchronize progress/history/changelog and current architecture/runbook after updates.
    Technical bug entries include symptom, root cause, function/line/config details,
    core fix, verification and recurrence tests without external reference paths/secrets.
12. Prepare local reviewable artifacts. Remote push, PR/merge and deployment require
    direct user authorization; a document reference alone does not authorize those actions.
13. Keep durable, referenced setup/verification utilities in `scripts/` tracked.
    Do not commit one-off local helper scripts or generated Python bytecode; ignore
    `__pycache__/` and `*.py[cod]`. Prefer the project's shell/Java stack for
    application work; retain Python only when an active project tool or gate uses it.
