# Testing and verification status

## Environment

- Java 21.0.12.1: `/home/tringuyen/.local/java/jdk-21`.
- Maven 3.9.11: `/home/tringuyen/.local/maven`.
- Writable dependency cache: `/tmp/claims-m2`; required plugin dependencies were resolved using the configured build environment.
- Docker CLI/Compose and a Docker daemon are available. Direct sandbox socket access and default loopback access return `Operation not permitted`; the documented operations were run through authorized escalated tool invocations. No socket permissions, groups, apt locks, or daemon configuration were changed.
- No Maven Wrapper is present. A wrapper would still require Java; the installed local Maven was used.

## Final verification run

| Command/check | Result | Evidence |
|---|---|---|
| `env JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/maven/bin:$PATH mvn -o -Dmaven.repo.local=/tmp/claims-m2 -B -Pquality clean verify` | PASS — 120 unit tests and 9 PostgreSQL/Testcontainers integration tests; 0 failures, errors, or skips; all five Liquibase changesets applied | [`quality-final.log`](../../reports/verification/quality-final.log) |
| JaCoCo from the successful clean run | PASS — 892/916 lines = 97.3799%; 549/609 branches = 90.1478% | `target/site/jacoco/jacoco.xml`; exact counts in [`T08-final.json`](../../reports/verification/T08-final.json) |
| `bash scripts/setup.sh` (second run with existing `.env` and volume) | PASS — existing DB healthy, app image rebuilt, app healthy; existing database retained 2 claims before the next scenario run | [`compose-repeat-setup.log`](../../reports/verification/compose-repeat-setup.log) |
| `bash scripts/setup.sh` (final build from normalized migration source) | PASS — Docker rebuilt the final app image; database and app both reported healthy | [`compose-final-setup.log`](../../reports/verification/compose-final-setup.log) |
| `bash scripts/smoke.sh` after repeat setup | PASS — authenticated health and claims API checks | [`api-smoke-final.log`](../../reports/verification/api-smoke-final.log) |
| `python3 scripts/run-scenarios.py` after repeat setup | PASS — 20/20 live HTTP checks, including authentication, CSRF, ownership/role restrictions, idempotent create/replay, full workflow progression, payment outbox confirmation, closure, list visibility, and audit | [`e2e-scenarios-final.log`](../../reports/verification/e2e-scenarios-final.log) |
| `python3 -m py_compile scripts/run-scenarios.py scripts/workflow/verify_workflow.py && python3 scripts/workflow/verify_workflow.py && python3 -m unittest discover -s scripts/workflow -p test_verify_workflow.py -v` | PASS — 12 validator unit tests; 13 manifest tasks pass structural validation | command output recorded in current task trace; validator restored from authoritative workflow contract |
| Docker Compose service status and claim count | PASS — app and database healthy; existing rows retained across repeat setups; four claims present after three unique scenario executions | [`T08-final.json`](../../reports/verification/T08-final.json) |

The successful quality command prints `running test case N/120` for unit tests and `running test case N/9` for integration tests. No production code was excluded to meet coverage. Meaningful tests cover boundary validation, authorization, audit immutability, idempotency, optimistic concurrency, transaction rollback across claim/audit/receipt/payment/outbox, payment outbox retries, and service behavior.

## Independent review

The integrated read-only reviewer found no critical or high severity implementation issue. It identified a possible race in the new compare-and-set test; both workers now use the same pre-captured version, and a focused rereview confirmed the finding is closed. The same-key advisory-lock test uses a bounded 250 ms negative wait, so it retains a small timing sensitivity. The reviewer recommended longer synchronization if that test flakes; its final receipt visibility assertion also verifies the committed result. Host-managed LLM payloads were not accessible: raw payload `NOT_CAPTURED`, trace audit `PARTIAL`. Preserved review evidence: [`final-integrated-review.md`](../submission/final-integrated-review.md) and the matching entries in `llm-interactions.jsonl`.

## Remaining checks and limits

| Exact command/check | Status | Reason |
|---|---|---|
| `mvn -B -Pquality clean verify` (without explicit local Java/Maven/cache paths) | NOT_RUN | This host's default PATH/cache differ; final verified invocation is listed above. |
| `bash scripts/run-scenarios.sh` | NOT_RUN | The authoritative runner is Python `scripts/run-scenarios.py`; no shell wrapper is defined. |
| Full 29-case `C14-01`–`C14-29` live end-to-end matrix as one suite | NOT_RUN | The 20-case API scenario runner covers the main HTTP lifecycle; remaining detailed business boundaries are covered in focused unit/integration tests, not one live API scenario matrix. |
| JMeter `claims-100.jmx` and `claims-500.jmx` load profiles | NOT_RUN | No load profile execution was requested or performed in this verification run; no capacity/SLA claim is made. |
| Trace/span propagation verification | NOT_RUN | The app records correlation IDs and metrics; no tracing bridge is configured, and observed trace/span values are null. |

The final integrated implementation is **not marked accepted or complete**. The precise run evidence, source scope hash, test counts, environment details, and known limits are recorded in [`T08-final.json`](../../reports/verification/T08-final.json). The historical failures in earlier verification attempts are retained in their original reports; they are superseded by the successful final run above, not deleted.
