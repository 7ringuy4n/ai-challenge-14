# Current Task

## Goal

Complete REQ-017's mandatory JMeter, browser, live authorization, requirement traceability, and local LLM audit gates. Merge `develop` into `main` only if every mandatory acceptance criterion genuinely passes; never push. Full historical host-managed payload capture is mandatory and cannot be waived.

## Current status

in_progress

## Current step

Commit corrected implementation and complete regression evidence on develop

## Completed

- [x] User approved configurable local member identities; REQ-018 and exclusive T14 paths are registered before implementation
- [x] Added the T14 tests first for two distinct MEMBER principals, default isolation, and invalid/duplicate identity rejection
- [x] Implemented validated CLAIMS_MEMBER_USERNAMES with member-demo default, explicit Compose/application wiring, and distinct configured MEMBER principals; test cases cover valid, duplicate, malformed, and empty input
- [x] Recorded user confirmation as PROMPT-20261017-019 and REQ-019; source config and focused test already implemented
- [x] Added real Chrome CDP workflow and live API/DB verifier under T14 owned paths; fixed CSRF redaction and static syntax checks
- [x] Full final-source Maven quality run passed 126 unit plus 9 PostgreSQL/Testcontainers tests, six migrations, and coverage gates (97.0134% line / 90.2022% branch).
- [x] Fresh live evidence passed: 29 API cases, 17 Chrome cases, 7 two-member ownership checks, Postman, smoke, and OTel HTTP-to-outbox correlation.
- [x] Independent review P2 was fixed and rereviewed PASS; the optimized live cross-member harness passed.
- [x] Exporter security fixes independently rereviewed PASS; 62 local session transcripts hash-verified and contained; workflow manifest and audit integrity pass. Reviewed source/config diffs and staged evidence on develop.
- [x] Seven JMeter profiles on c6dccfe independently reviewed PASS with clean source hashes, 1,564,951 samples and approved thresholds; source changed afterward due P2, so those results are historical for c6dccfe only.
- [x] Fixed P2 member username length boundary; focused security test passed 5/5 and full Maven quality passed 127 unit + 9 integration tests, six migrations, 97.0195% line / 90.2326% branch. Independent rereview PASS.
- [x] Fixed and independently rereviewed the 127-character member principal boundary. Full Maven quality passed (127 unit + 9 PostgreSQL integration, six migrations, 97.0195% line/90.2326% branch); default smoke, combined 29-case API, Postman, real Chrome 17/17 and live two-member 7/7 passed.

## Next exact step

Commit corrected source/evidence on develop and execute all seven JMeter profiles against that exact clean source commit.

## Blockers

- Historical raw host-managed model payloads remain inaccessible and mandatory for final acceptance; prior JMeter results must be rerun after the source correction.

## Known constraints

- Default sandbox restricts Docker and loopback; approved escalated invocations were used without modifying permissions.

## Relevant files

- `reports/verification/REQ-017-current.json`

## Verification commands

```bash
Reports and task manifest reflect NOT_ACCEPTED pending final-source JMeter rerun and mandatory raw-payload audit blocker.
```

## Checkpoint metadata

- Updated (UTC): 2026-10-17T06:06:40Z
- Branch: `develop`
- Commit: `c6dccfe`
