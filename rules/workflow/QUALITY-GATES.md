# Quality gates

Task state is not test evidence. A worker submission becomes accepted only after
coordinator-run checks, independent review and source-matched acceptance records.

## Architecture and review

- State Machine: configured string states/edges; demonstrate a config-only addition.
- Strategy/Factory: guard/effect registries resolve supported capabilities and reject unknown IDs.
- SRP/DIP/ISP: pure engine, transactional application, small repository ports and MyBatis adapters.
- Immutable values: defensive nested facts and canonical PostgreSQL timestamp precision.
- Authorization: principal actor, trusted facts, member ownership, protected system identity.
- Consistency: atomic state/audit/payment/receipt/outbox, row lock/CAS, idempotent replay.
- Audit: runtime append/read only, trigger defense, no cascading deletion.
- Reliability: after-commit mock effects, retry state, no false exactly-once claim.
- Observability: real trace/span context, outcome/latency fields, no secret/medical-content logging.
- UI: dynamic states/choices, accessible forms/focus, responsive layout, escaping and clear errors.

## Acceptance cases

- C14-01: YAML loads eight initial states and nine edges with expected role/guard/effect metadata.
- C14-02: Happy path closes with five successful transition audit entries.
- C14-03: Rejection path includes reason/appeal instructions and eligible system closure.
- C14-04: Information loop resets assessment timer and clears stale completion before approval.
- C14-05: Illegal SUBMITTED-to-APPROVED edge reports source, target and available choices.
- C14-06: Unauthorized role fails at engine/application boundary without state/audit/outbox mutation.
- C14-07: Missing or invalid required documents fail with specific guard details.
- C14-08: Assessment cannot start without assigned assessor.
- C14-09: Incomplete report cannot approve/reject.
- C14-10: Amount equals limit passes; excess/negative/null/invalid currency fails appropriately.
- C14-11: Rejection and information request require nonblank reason/description.
- C14-12: Old information receipt fails; fresh server-stamped receipt permits return.
- C14-13: Payment initiation needs persisted request; closure needs confirmation/reference.
- C14-14: Appeal expiry boundary and member acknowledgement paths behave deterministically.
- C14-15: Three information requests succeed; fourth has exact escalation message; claims are independent.
- C14-16: Audit fields/order/claim scoping remain accurate and complete.
- C14-17: Runtime audit UPDATE/DELETE denied; owner trigger protects accidental mutation; no cascade.
- C14-18: Audit/outbox failure rolls back state, counter and payment request.
- C14-19: Rollback dispatches nothing; after-commit failure retains observable retry.
- C14-20: Concurrent same-version commands have one winner and no duplicate audit/payment.
- C14-21: Same actor/claim/key/payload replays result; different payload conflicts.
- C14-22: New state/edge works with configuration-only diff and dynamic interface options.
- C14-23: Duplicate/dangling/unknown guard/effect configuration fails startup clearly.
- C14-24: Terminal state has no outgoing choices.
- C14-25: HTTP401/403, ownership and system-role spoof protection.
- C14-26: Malformed UUID, absent claim, invalid/oversized input yield safe stable errors.
- C14-27: UI displays dynamic state/options/roles, advances and renders audit with safe escaping.
- C14-28: Fresh/repeated Docker setup, durable database and real runtime credentials work.
- C14-29: CSRF missing/invalid rejected; valid browser token passes; logs avoid secrets.

## Test execution and evidence

At least15 meaningful Java tests are required by the brief. Unit tests use JUnit5,
Mockito/AssertJ as appropriate. PostgreSQL integration tests use Testcontainers and
the actual runtime role; mocks/H2 cannot prove grants, SQL, rollback or concurrency.
Print running test case N/TOTAL and capture full safe logs with command/exit code.

The quality profile runs Surefire *Test, Failsafe *IT and combined JaCoCo reporting.
Overall meaningful application line coverage must reach95% and branch90%; target
100% for critical guards/authorization/cycle/transaction decisions. Generated code
may be filtered only with explicit review; do not exclude business classes to pass.
Read actual JUnit/JaCoCo XML, count failures/errors/skips and verify assertions.

```bash
mvn -B -Pquality clean verify
docker compose --env-file .env -f docker/compose.yml config --quiet
bash scripts/setup.sh
bash scripts/smoke.sh
bash scripts/run-scenarios.sh
```

Setup provisions only; tests are separate. Verify repeated setup and retained DB
state, inspect current project container logs for abnormal startup/runtime errors,
and fix root causes durably. Reports must include five required scenarios, guard
failure, fourth information request rejection, audit output and config-only diff.
Tie results to final source hashes. Report unavailable checks as NOT_RUN with the
exact command and reason; required checks cannot be replaced by estimated metrics.
Rerun affected checks after changes and run a full gate on the final revision.
