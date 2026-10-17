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

- Seven JMeter final-source full-profile reruns remain NOT_RUN; approved targets are P95 <=500ms, P99 <=1000ms, errors <=1%.
- REQ-018 local principal config, Chrome E2E, cross-member PostgreSQL ownership and grouped 29-case live suite now PASS; internal criterion-level HTTP coverage is still a mix of live and unit/PG evidence.
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

- [x] REQ-018 configurable local member principals implemented and two distinct members verified against live PostgreSQL.
- [x] Fresh full Maven quality run passed 126 unit + 9 Testcontainers tests; six Liquibase changesets; coverage 97.0134% line / 90.2022% branch.
- [x] Current combined live API run passed 29/29 with request/response transcript; real Chrome passed 17/17; two-member isolation passed 7/7; Postman and smoke passed.
- [x] Fresh trace evidence confirms HTTP context propagation through the asynchronous outbox and log correlation.
- [x] T14 reviewer P2 harness issue fixed and rereviewed PASS; optimized verifier run passes 7/7.
- [ ] Seven final-source JMeter profiles and exhaustive accessible local Codex session audit remain.
