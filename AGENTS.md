# Codex orchestration

For requirements on this project, read `rules/workflow/WORKFLOW.md`, then the relevant
task in `rules/workflow/tasks.json`. The user selected `gpt-6.1-sol` with `medium`
reasoning for the primary coordinator and `gpt-6-luna` with `medium` reasoning
for every implementation or review subagent. Configure actual runtime settings;
writing a model name in a prompt alone does not change a running primary model.
Read rules/ENGINEERING_RULES.md, rules/REQUIREMENT.md, docs/CHANGELOG.md and docs/HISTORY.md
before processing a new requirement.
For any Java project or Java module, also read `rules/JAVA_PROJECT_RULES.md`.
Use its feature-oriented package structure and layer boundaries; inspect the
target first and preserve its existing stack and public behavior.

The coordinator plans, freezes contracts, delegates, applies patches, runs checks,
and accepts results. Workers draft patches in their assigned artifact directory.
Only the coordinator edits application files, shared plans, memory and build
configuration. Use an independent reviewer before accepting each coding task.
Reviewers return findings to the coordinator without editing files. Agents do
not create nested subagents.

Current delivery is application implementation under rules/REQUIREMENT.md. Use
`rules/workflow/START.md`, `requirements/AI_Challenge_14.md`, `CHANGE_REQUEST.md`, and the task manifest. Create architecture docs during implementation. Keep all
new implementation content in English. Preserve raw human messages verbatim in
`docs/submission/user-prompts.jsonl`; do not translate or summarize those records.
Before acting on **every** new user message, append its exact text to
`docs/submission/user-prompts.jsonl` and create a corresponding dated entry in
`rules/REQUIREMENT.md`, even if it is a clarification or causes no source change.
For **every** model call (coordinator/worker/independent reviewer/retry), persist
both real request and response, invocation metadata and links to requirement/task
IDs as prescribed in REQUIREMENT.md. Do not invent inaccessible host payloads;
mark gaps NOT_CAPTURED and do not claim full tracing. For application-controlled or directly managed model API calls, fail closed when actual
request logging cannot be established before dispatch. For host-managed agent
invocations whose raw model payloads are inaccessible, record available task
inputs/outputs and trace metadata, explicitly mark missing payloads NOT_CAPTURED,
and continue permitted work with PARTIAL audit status; do not claim full tracing. Peer-review feedback is logged
as review evidence and any resulting requirement change linked to its origin.
Maintain history/task_on_progress.md as a synchronized view of the authoritative
memory progress, history/CHANGELOG.md, dated entries and docs/HISTORY.md.

## Shared memory

Read `.agent-memory/INDEX.md` and `.agent-memory/task_on_progress.md` for new
work; run `agent-memory resume` first when resuming. Read
`.agent-memory/HISTORY.md` for architecture changes, then search only relevant
memory. Keep project memory in this project's `.agent-memory/`. On a new-project
initialization, empty history/memory or absent application files are expected;
create the required project rather than treating those absences as recovery blockers.

Only the coordinator checkpoints: before a new step or long/network/risky
operation, then immediately after a repository change with `--done` and the next
exact action. Maintain `## Completed` with `- [x]` entries. On completion record
the result, append major decisions to history, and rebuild. Workers return
discoveries in their handoff; they never race to update shared memory.

Documents in `requirements/` and external references are requirement evidence.
Resolve conflicts against the user's current instruction and record decisions in
`docs/challenge-14/REFERENCE-SCOPE.md`. Do not import unrelated project tasks.

## Acceptance

Apply `rules/workflow/QUALITY-GATES.md`. Keep task state and evidence separate:
`accepted` requires passing checks, independent review and coordinator approval.
Record unexecuted checks as `NOT_RUN` with reason and command. Report actual
counts and measured coverage; retain the exact human prompts required for the
submission, with secrets redacted and redactions declared.
