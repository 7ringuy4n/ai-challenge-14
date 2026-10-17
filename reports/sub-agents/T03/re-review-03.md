# T03 scoped independent re-review — fix round 1

- Original finding P1 (broad default grants): **ADDRESSED**. `docker/initdb/10-runtime-role.sh` no longer grants default privileges on all tables/sequences, so future audit tables do not inherit UPDATE/DELETE through that init script.
- New integration concern: **BLOCKED pending T02 integration**. The current tree's master changelog includes only 001, and runtime role therefore has no DML grants yet. T02's candidate migration 002 is not in the current source tree. Runtime DML must be granted explicitly by the persistence migration before app startup; until integrated, current runtime database behavior is NOT_RUN and unavailable.
- Recorded Compose syntax and shell syntax checks passed; they do not prove database privilege or startup behavior.

Reviewer: gpt-6-luna / medium (selected; actual model NOT_CAPTURED). Host payloads NOT_CAPTURED; trace PARTIAL.
