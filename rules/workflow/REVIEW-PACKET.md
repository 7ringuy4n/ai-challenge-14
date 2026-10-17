# Independent reviewer packet

Use a fresh gpt-6-luna/medium agent different from the implementation author.
Supply task/requirement IDs, contracts, changed paths, reviewed source hash and
actual focused-test logs. Reviewers read only and return findings; they do not
edit files, run shared builds/DB/Docker/Git mutations or spawn agents.

Check actual code and assertions against each criterion. Inspect configuration
extensibility, meaningful patterns, authorization, trusted facts, transaction and
audit integrity, concurrency/idempotency, failure handling and real test evidence.
Do not treat the author's success claim as execution evidence. If more reproduction
is needed, return the command to the coordinator and inspect its actual result.

Return PASS/FAIL/BLOCKED, per-criterion evidence, actionable severity/file:line/
trigger/consequence/fix findings, missing tests and unexecuted checks. Coordinator
owns acceptance and resolution. After fixes, scoped rereview checks addressed
findings and new breakage; final review checks the whole integrated application.
