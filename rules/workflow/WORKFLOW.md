# Coordinator and worker workflow

Coordinator: gpt-6.1-sol / medium. Every implementation and review subagent:
gpt-6-luna / medium. Use at most three concurrent subagents outside the coordinator,
subject to host limits. Pass explicit model/effort and fresh context when spawning.
Instructions alone do not change the active primary model or host permissions.

## Requirement loop

1. Read rules/REQUIREMENT.md, applicable rules, current docs/history and authoritative
   memory progress. For **each** new human message, append the verbatim prompt to
   `docs/submission/user-prompts.jsonl` and a dated requirement-change entry to
   `rules/REQUIREMENT.md` **before** implementation, even for follow-up requests.
   Establish append-only interaction evidence recording before dispatch. Enforce
   full pre-dispatch request capture for directly managed/model API calls; for
   host-managed calls preserve accessible task packets and execution evidence
   and document any unavailable raw payloads as NOT_CAPTURED.
2. Update acceptance criteria, to-do/DAG, inputs/outputs, exclusive patch paths,
   required checks and estimate. Freeze/compile shared interfaces before consumers.
3. For directly controlled LLM API invocations (including workers, reviewers and
   retries), persist the actual outbound request before dispatch and the inbound
   response/error/partial output afterward; fail closed if pre-dispatch request
   logging is unavailable. For host-managed coordinator/worker/reviewer calls,
   persist every exposed task packet, input/output, model/attempt metadata and
   review artifact. If the host hides raw model payloads, record the affected
   interaction ID, reason and `NOT_CAPTURED` fields, set audit status `PARTIAL`,
   and continue authorized dispatch (no fabrication or COMPLETE audit claim).
   Link interactions -> requirements -> tasks/attempts. Protect secrets, dispatch
   only when dependencies are accepted, and pass source/contract hashes, role
   and exclusive task/attempt artifact directory.
4. Workers read source and draft candidate.patch plus handoff.json in their own
   artifact directory. They do not modify source, plans, memory or build output,
   run builds/Docker/DB/Git mutations, or spawn other agents.
5. Coordinator checks patch target ownership, base/contract hashes and full content,
   dry-runs the patch and applies it serially. Rebase stale patches before apply.
6. Coordinator runs focused checks, captures command/exit code/source hash/report,
   prints test progress and fixes core causes through the owning worker.
7. A fresh independent reviewer inspects actual source/assertions/results and
   reports criteria coverage and actionable findings with file:line. Resolve
   correctness/security/requirement findings and rereview affected changes.
8. Coordinator verifies interaction evidence and independent-review trace links,
   recording missing host-managed payloads as `NOT_CAPTURED` and audit `PARTIAL`.
   `PARTIAL` alone does not prohibit implementation acceptance, provided mandatory
   tests pass, review is independent, source/evidence match, and no explicit full-
   capture acceptance condition remains unmet. Never report full traceability when
   any required payload is missing. Coordinator accepts only when
   every required command has final PASS/exit0 and other gates are satisfied. Update manifest,
   checklist, memory and synchronized history after acceptance.
9. Run final integrated gates/review, update current architecture/runbook/history,
   and prepare source/config/scenarios/audit/diff/raw-conversation submission.

## One source writer

Workers write only rules/workflow/artifacts/TASK/ATTEMPT/. allowed_paths in the manifest
constrain the intended patch targets, not direct filesystem edits. Coordinator
owns source/shared files and serializes builds, database and Docker operations.
Independent task paths cannot overlap or contain one another. Shared ports,
pom/application YAML/changelog/manifest/memory changes go through the coordinator.

Validate candidate scope before apply:

```bash
python3 scripts/workflow/verify_workflow.py --patch T01 rules/workflow/artifacts/T01/01/candidate.patch
git apply --check rules/workflow/artifacts/T01/01/candidate.patch
git apply rules/workflow/artifacts/T01/01/candidate.patch
```

The host apply-patch tool can integrate validated text changes if git apply is
unavailable. Path checks do not review code behavior; content review remains
required. Single-writer integration prevents source/build write races; contract
and integration tests address semantic conflicts. Agent instructions are a
protocol, not an independently enforced filesystem sandbox.

## State, evidence and recovery

planned → running → submitted → in_review → accepted. Failures require rework;
unavailable prerequisites require blocked with the exact recovery command.
Every attempt gets a new artifact directory; old and new attempts never run
concurrently. After repeated failures, repair the contract/task decomposition
while preserving the user's model choice. Do not silently change models.

Verification evidence needs command, exit_code0, source_hash and artifact. Review
evidence includes reviewer_agent distinct from author; coordinator approval is
separate. All required commands need final PASS on the accepted source; NOT_RUN
cannot stand in for a pass. Final-review tasks compare reviewer against source
authors rather than requiring a review of the review itself. Validator checks
structure, not artifact signatures or full status history; coordinator reads
actual logs/files and checks runtime identity.

Only coordinator checkpoints shared memory before/after steps. Resume from
agent-memory resume, inspect active agents/leases/artifacts/base hashes, and
continue unfinished work. history/task_on_progress.md is a synchronized view;
.agent-memory/task_on_progress.md is authoritative. Update history changelog,
dated technical entries and docs/HISTORY.md after project updates.

Use TASK-PACKET.md, REVIEW-PACKET.md and QUALITY-GATES.md for dispatch/review.
