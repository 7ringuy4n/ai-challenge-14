# Current Task

## Goal

Complete REQ-017 acceptance under owner-approved REQ-020; preserve the original P95 FAIL and host raw-payload PARTIAL status. Merge accepted develop evidence into the existing main only after all gates and independent review pass. Never push.

## Current status

accepted_with_performance_exception; local merge pending

## Current step

Commit reviewed acceptance evidence on develop, merge into existing main, and record the verified merge without pushing

## Completed

- [x] Captured current 29/29 live scenario console plus HTTP/trace artifacts in REQ-017-final-live; the initial default-sandbox network denial after 0/29 is preserved separately in sandbox-attempt-blocked.log
- [x] Captured one combined 29/29 live run with current console, HTTP exchanges, and trace context; fixed stale performance sample and Newman request counts and task evidence links
- [x] T15 independent rereview found that 29/29 run trace context had no correlated exported collector spans; earlier trace evidence was from a different run
- [x] Independent T15 review 04 accepted the reporting task; no documentation findings remain. Latest inventory export has 65 sessions/8,483 records and all transcript hashes validate.
- [x] Synchronized final audit to 65 sessions/8,483 records and 101 ledger rows/60 IDs; marked T15 accepted after independent review 04 while retaining project NOT_ACCEPTED blockers; updated changelogs, dated requirement history, and progress mirrors
- [x] Committed reporting, performance evidence, latest local audit export, and T15 rereview to develop at 70aa7d8. Workflow validator and artifact parsing passed after commit; working tree was clean.
- [x] Appended the exact new acceptance-policy prompt as PROMPT-20261017-020 in docs/submission/user-prompts.jsonl
- [x] Read workflow/engineering/requirement/build/history and resumed the saved task; confirmed branch develop HEAD 8f6f5ad and planning main 521cb14 with origin configured
- [x] Registered REQ-020 as the revised policy requirement and added T17; reassigned T13/T16 to evidence-based hardware and proof-of-work assessment while preserving historical results
- [x] Added a sanitized aggregate monitor script collecting host CPU/memory/swap/PSI/thermal counters, JMeter process CPU/RSS, target container CPU/memory, actuator Hikari/GC/JVM metrics, and PostgreSQL waits/deadlocks/outbox without request or credential logging
- [x] Instrumented run on clean source 0435e1b completed: 76,251 samples, P95 638ms/P99 879ms/0 errors. Captured host CPU peak 99.84%, load average peak 28.47 on 8 CPUs, JMeter CPU/RSS, app/Hikari/GC, DB wait/lock/outbox, RAM/swap and thermal throttle counters
- [x] Preserved original stress FAIL and added REQ-020 monitored diagnostic plus evidence-backed local hardware assessment; CPU saturation confirmed while RAM, thermal, GC, DB lock, Hikari timeout, and outbox evidence were reconciled
- [x] Refreshed the 65-session export and verified transcript hashes, unique IDs, and contained unique paths; recorded the active coordinator source log growth qualification and raw payload PARTIAL status
- [x] Reran logged Maven quality regression: 127 unit + 9 PostgreSQL/Testcontainers, six migrations, 97.0195% lines / 90.2326% branches; workflow validator passes 18 dependency-valid tasks
- [x] Archived the historical REQ-017 acceptance decision and prepared the revised acceptance report and traceability matrix
- [x] Independent reviews 01 and 02 found task-state and criterion inconsistencies; preserved both findings and corrected T13/T16 status and T13 exception criterion
- [x] Independent review 03 passed the reconciled decision; coordinator marked REQ-020 ACCEPTED_WITH_PERFORMANCE_EXCEPTION while retaining P95 FAIL, raw payload PARTIAL/NOT_CAPTURED, and production-scale UNVERIFIED
- [x] Final local session inventory contains 67 sessions / 8,923 accessible records; all transcript hashes, IDs, and contained unique paths pass; active source log growth is disclosed. Ledger is 105 rows / 64 IDs, 63 with explicit requirement/task links

## Next exact step

Commit staged reviewed evidence to develop, verify clean tree, then perform a normal local merge into existing main; record actual hashes and do not push

## Blockers

- Commit to `develop` and local merge into existing `main` are authorized and pending execution; no push.
- Production-scale performance remains UNVERIFIED; this is explicitly not represented as passing.

## Known constraints

- None.

## Relevant files

- `reports/acceptance/FINAL-ACCEPTANCE.md`
- `reports/verification/REQ-017-current.json`
- `reports/verification/REQ-017-jmeter-final`

## Verification commands

```bash
python3 scripts/audit/export-codex-sessions.py
python3 scripts/workflow/verify_workflow.py
python3 -m json.tool reports/verification/REQ-017-current.json
python3 -m json.tool reports/verification/REQ-017-requirement-matrix.json
```

## Checkpoint metadata

- Updated (UTC): 2026-10-17T07:22:00Z
- Branch: `develop`
- Commit: `8f6f5ad`
