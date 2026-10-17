# Current Task

## Goal

Complete REQ-017's remaining acceptance gates; merge develop into main only if every mandatory gate passes. Never push. Historical host-managed raw model payload capture is mandatory and cannot be waived without an explicit user policy decision.

## Current status

in_progress

## Current step

REQ-017 evidence committed; acceptance gates remain open

## Completed

- [x] Ran temporary Hikari-40 and Tomcat-500 stress variants; both worsened throughput and latency. Independent reviewer advises against tuning and warns these sequential runs on evolving database state do not prove cause.
- [x] Restored documented app defaults; health check passes.
- [x] Refreshed local Codex export to 65 sessions/8,483 accessible records; all transcript hashes and path containment verify. Ledger has 101 rows/60 interaction IDs; host-managed raw payloads remain NOT_CAPTURED.
- [x] Updated final matrix and acceptance report; overall status remains NOT_ACCEPTED; do not merge or push.
- [x] Updated final acceptance matrix: JMeter six PASS/one FAIL; 64-session local audit / 8,172 accessible records PASS; full host-managed raw payload gate remains BLOCKED
- [x] Recorded independent performance review and rejected unsupported pool/thread changes; documented shared-host performance failure and no merge
- [x] Captured current 29/29 live scenario console plus HTTP/trace artifacts in REQ-017-final-live; the initial default-sandbox network denial after 0/29 is preserved separately in sandbox-attempt-blocked.log
- [x] Captured one combined 29/29 live run with current console, HTTP exchanges, and trace context; fixed stale performance sample and Newman request counts and task evidence links
- [x] T15 independent rereview found that 29/29 run trace context had no correlated exported collector spans; earlier trace evidence was from a different run
- [x] Independent T15 review 04 accepted the reporting task; no documentation findings remain. Latest inventory export has 65 sessions/8,483 records and all transcript hashes validate.
- [x] Synchronized final audit to 65 sessions/8,483 records and 101 ledger rows/60 IDs; marked T15 accepted after independent review 04 while retaining project NOT_ACCEPTED blockers; updated changelogs, dated requirement history, and progress mirrors
- [x] Committed reporting, performance evidence, latest local audit export, and T15 rereview to develop at 70aa7d8. Workflow validator and artifact parsing passed after commit; working tree was clean.

## Next exact step

Obtain a controlled same-data benchmark environment and supported host-managed LLM payload export or policy decision; rerun failed gates before any acceptance or merge.

## Blockers

- JMeter stress P95 564 ms exceeds approved 500 ms; host-managed raw LLM payloads remain NOT_CAPTURED and full audit is mandatory.

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

- Updated (UTC): 2026-10-17T06:49:11Z
- Branch: `develop`
- Commit: `70aa7d8`
