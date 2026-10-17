# Current Task

## Goal

Complete REQ-017's mandatory JMeter, browser, live authorization, requirement traceability, and local LLM audit gates. Merge `develop` into `main` only if every mandatory acceptance criterion genuinely passes; never push. Full historical host-managed payload capture is mandatory and cannot be waived.

## Current status

in_progress

## Current step

Commit verified implementation and evidence on develop

## Completed

- [x] Recorded exact SHA-256 hashes for all seven JMX plans and seven property files before any final reruns
- [x] User approved local member principal configuration; logged as PROMPT-20261017-018 and registered REQ-018 under T14
- [x] Captured the 14 exact JMX/property hashes before starting any benchmark; updated requirement, task dependencies/ownership, history and conditional merge policy
- [x] User approved configurable local member identities; REQ-018 and exclusive T14 paths are registered before implementation
- [x] Added the T14 tests first for two distinct MEMBER principals, default isolation, and invalid/duplicate identity rejection
- [x] Implemented validated CLAIMS_MEMBER_USERNAMES with member-demo default, explicit Compose/application wiring, and distinct configured MEMBER principals; test cases cover valid, duplicate, malformed, and empty input
- [x] Recorded user confirmation as PROMPT-20261017-019 and REQ-019; source config and focused test already implemented
- [x] Added real Chrome CDP workflow and live API/DB verifier under T14 owned paths; fixed CSRF redaction and static syntax checks
- [x] Full final-source Maven quality run passed 126 unit plus 9 PostgreSQL/Testcontainers tests, six migrations, and coverage gates (97.0134% line / 90.2022% branch).
- [x] Fresh live evidence passed: 29 API cases, 17 Chrome cases, 7 two-member ownership checks, Postman, smoke, and OTel HTTP-to-outbox correlation.
- [x] Independent review P2 was fixed and rereviewed PASS; the optimized live cross-member harness passed.
- [x] Exporter security fixes independently rereviewed PASS; 62 local session transcripts hash-verified and contained; workflow manifest and audit integrity pass. Reviewed source/config diffs and staged evidence on develop.

## Next exact step

Commit verified implementation and evidence on develop, then execute seven JMeter profiles from a clean checkout of that commit.

## Blockers

- Full host-managed raw model payload history remains unavailable and is a mandatory acceptance gate.

## Known constraints

- Default sandbox restricts Docker and loopback; approved escalated invocations were used without modifying permissions.

## Relevant files

- `reports/verification/REQ-017-current.json`

## Verification commands

```bash
python3 scripts/workflow/verify_workflow.py
```

## Checkpoint metadata

- Updated (UTC): 2026-10-17T05:22:30Z
- Branch: `develop`
- Commit: `5cb58a8`
