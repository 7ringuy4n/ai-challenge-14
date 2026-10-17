# Implementation worker packet

For each dispatch supply: task ID/attempt, root path, acceptance case IDs, exact
source/contract files, dependency acceptance, consumed/produced signatures,
allowed patch paths, exclusive artifact directory, base hashes, test cases and
required verification commands. Use gpt-6-luna/medium with fresh context.

Workers read source, draft meaningful implementation/tests as candidate.patch,
and write handoff.json only in their assigned artifact directory. They do not
edit source/shared files or run builds, Docker, DB/Git mutations or nested agents.
The coordinator integrates and runs checks; worker proposals are not execution
results. Report missing shared interfaces immediately with a concrete proposal.

Handoff fields: task_id,attempt,status,model,reasoning_effort,contract_version,
base_files,patch_paths,criteria_addressed,tests_added,checks_executed,
checks_not_run,decisions,risks,requested_shared_changes. Use NOT_RUN for commands
not executed. Unified text patches use a/path,b/path and /dev/null for new/deleted
files. Rename/binary/symlink/mode changes are coordinator-owned separate actions.
