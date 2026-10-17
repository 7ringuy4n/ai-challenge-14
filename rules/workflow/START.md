# Execute the Java Challenge14 workflow

Read `rules/REQUIREMENT.md`, `AGENTS.md`, `WORKFLOW.md`,
`requirements/AI_Challenge_14.md`, `CHANGE_REQUEST.md`, and
`rules/workflow/QUALITY-GATES.md`. This workspace may be a new-project
initialization with no application sources or historical application docs yet;
create those deliverables from the current requirements. Current task state is
in `tasks.json`; keep planning artifacts consistent with it. The user has
authorized local implementation and verification in this workspace.

## Models

Select GPT6.1 SOL / Medium in the coordinator's actual Codex session. Every worker
and independent reviewer uses GPT6 Luna / Medium. Configuration samples are under
codex/config.toml and codex/agents/; merge them into a writable project .codex
configuration without replacing unrelated settings. The current protected .codex
has not been modified. The host collaboration tool can explicitly spawn Luna with
model=gpt-6-luna, reasoning_effort=medium and fork_turns=none.

Model names in prompts do not switch the running primary model. A host that cannot
provide the selected model must report the limitation rather than silently
substitute. Agents inherit host permissions. Official schema guidance:
https://learn.chatgpt.com/docs/agent-configuration/subagents

## Reusable execution prompt

```text
Implement the requirement in rules/REQUIREMENT.md using AGENTS.md and rules/workflow/WORKFLOW.md.
Use SOL medium as coordinator and Luna medium for every worker/reviewer. Maintain
the to-do/dependency graph, freeze contracts, dispatch independent ready tasks,
keep one source writer and serial builds, get independent review, and accept only
actual passing checks on the reviewed source. Continue within the authorized scope.
Preserve my messages verbatim in the separate AI conversation log. Keep English
implementation, synchronized progress/history, current docs and honest NOT_RUN
results. Fix core causes and rerun affected tests.
```

## Workflow guard checks

Python3.11+ standard library only:

```bash
python3 scripts/workflow/verify_workflow.py
python3 scripts/workflow/verify_workflow.py --ready
python3 -m unittest discover -s scripts/workflow -p test_verify_workflow.py -v
```

Accepted evidence must cover each required command, matching source hash, real
report and independent reviewer. The validator cannot authenticate artifact/hash
provenance or the full status ledger. Coordinator verification remains required.
Java application results are separate from these Python workflow safeguard tests.
