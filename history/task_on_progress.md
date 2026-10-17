# Current Task

## Goal

Complete REQ-017's mandatory JMeter, browser, live authorization, requirement traceability, and local LLM audit gates. Merge `develop` into `main` only if every mandatory acceptance criterion genuinely passes; never push. Full historical host-managed payload capture is mandatory and cannot be waived.

## Current status

in_progress

## Current step

Commit REQ-017 scope, ownership plan and immutable JMeter pre-run hashes

## Completed

- [x] Final clean Maven quality run: 123 unit + 9 PostgreSQL/Testcontainers tests, zero failures/errors/skips, six migrations; 97.0464% line / 90.1116% branch.
- [x] Independent review found no critical/high issue; token redaction and stale test name findings fixed. Host-managed LLM payloads remain NOT_CAPTURED/PARTIAL.
- [x] One live PostgreSQL-backed 29-case run, JMeter profiles, OTel trace export, Newman run, six-migration clean suite, coverage and independent rereview are complete with open acceptance gaps documented
- [x] Refreshed REQ-015 report, traceability, Postman, operations/architecture/testing docs, history, changelog, task manifest and reviewer handoffs.
- [x] Validated report JSON, SVG XML, all five diagram source/render sets, 29 numbered console cases, 31 redacted HTTP exchanges, JMeter comparison, and workflow validator.
- [x] Reversibly compressed retained JMeter/Collector raw outputs, reducing the report package from over 1 GB to about 100 MB.
- [x] Updated current REQ-015 verification record and synchronized task manifest/history, with evidence checks and open gates explicit
- [x] Closed JMeter installer review findings: captured full version output with `pipefail` and version assertion, tested malformed target refusal, fixed evidence links, and recorded independent rereview with no critical/high findings
- [x] Recorded the attached REQ-017 prompt verbatim as PROMPT-20261017-017, added requirement REQ-017, and split active verification into coordinator-owned T13–T16 workstreams with separate evidence paths
- [x] REQ-017 is in the requirement ledger, work manifest, changelog and history; conditional merge scope is reconciled; task stays running
- [x] Recorded exact SHA-256 hashes for all seven JMX plans and seven property files before any final reruns
- [x] Captured the 14 exact JMX/property hashes before starting any benchmark; updated requirement, task dependencies/ownership, history and conditional merge policy

## Next exact step

Run all seven JMeter profiles serially from the clean resulting develop commit, then compare every plan hash with the recorded pre-run set

## Blockers

- Full JMeter profiles used the final business/tracing implementation before a logging-only trace-context adjustment; a full byte-identical rerun remains NOT_RUN.
- Browser UI and live two-member ownership scenarios remain NOT_RUN; grouped live cases do not separately execute every requirement bullet.
- Host-managed raw LLM request/response payloads remain NOT_CAPTURED; audit is PARTIAL.
- JMeter archive download/checksum/extraction command exit passed, but its exact timestamp was not captured; the separate executable version and target-guard checks have an exact recorded timestamp/result.
- REQ-017 gates remain pending: final-source load rerun, browser E2E, live two-member authorization, complete per-requirement matrix, and exhaustive local session/payload audit. Merge is prohibited until every one passes.

## Known constraints

- Default sandbox restricts Docker and loopback; approved escalated invocations were used without modifying permissions.

## Relevant files

- `reports/verification/REQ-017-jmeter-profile-hashes-before.txt`

## Verification commands

```bash
17-task manifest validates; git diff --check passes; no benchmark has run yet
```

## Checkpoint metadata

- Updated (UTC): 2026-10-17T04:38:07Z
- Branch: `develop`
- Commit: `e9e42e0`
