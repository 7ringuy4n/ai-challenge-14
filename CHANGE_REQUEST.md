# DECISION

- Implement my requirements.

# REQUIREMENT

- Read `rules/REQUIREMENT.md`, `AGENTS.md`, `docs/CHANGELOG.md`, `docs/HISTORY.md`, and the authoritative `.agent-memory/task_on_progress.md` before processing.
- Preserve every new user request verbatim in the separate AI conversation log and register a new dated entry in `rules/REQUIREMENT.md` before making changes, every time.
- Capture every LLM request and response during implementation, including coding agents, subagents, peer reviews, retries, and failures. Record timestamps, IDs, model information, and trace links. Mark unavailable evidence honestly.
- Update the to-do list, task dependencies, and exclusive patch ownership before implementation.
- Inspect relevant application and container logs.
- Reproduce reported issues before modifying code and fix the root cause.
- Test core behavior and report progress using `running test case N/TOTAL`.
- Update the authoritative task-progress file, history, changelog, and dated technical records after each update. Synchronize architecture and run instructions when affected.
- Follow all existing project rules. Do not duplicate or override authoritative rules in this template.

*Implement requirements in `/home/tringuyen/Documents/ai-challenge-14/requirements`.*

# WORKING DIRECTORY

- path: `/home/tringuyen/Documents/ai-challenge-14`

# GOAL

- Project can be used immediately without known blocking issues.

# ISSUES

-

# IMPROVE

-

# MUST

1. Archive completed progress and retain unfinished current-task items.
2. Register every new request and preserve the original conversation evidence.
3. Capture LLM request/response evidence throughout implementation and peer review.
4. Reproduce reported issues before changing code; fix the root cause.
5. Run setup on the applicable fresh or existing environment when setup changes.
6. Run meaningful focused, regression, and integration tests and independently review.
7. Record commands, environment, results, and any `NOT_RUN` or `NOT_CAPTURED` checks honestly.
8. Update requirements, task progress, history, changelog, architecture, and conversation logs.
9. Do not mark work complete without traceable implementation and verification evidence.
10. Enforce Git branching and workflow rules: Before implementing any changes, verify the Git working tree, update the local `main` branch from `origin/main`, and create or switch to a dedicated `develop` branch based on the latest `main`. Follow the project's Git workflow for all subsequent changes, including meaningful commits, branch synchronization, and merge procedures. Never implement directly on `main`, force-push, overwrite existing work, or merge without required tests and review. Record branch names, commit hashes, and Git operations for traceability. If `develop` already exists, verify its relationship to `main` and preserve its existing changes instead of recreating or resetting it.
