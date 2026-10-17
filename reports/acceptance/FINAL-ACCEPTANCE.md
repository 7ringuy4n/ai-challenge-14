# Final Acceptance — REQ-020 Revised Policy

**Current decision: `ACCEPTED_WITH_PERFORMANCE_EXCEPTION`; independent review-03 passed and `develop` was merged locally into existing `main`. Merge commit: `18f97aa9c8296fdc107a02192aa4e8e62dd45d78`. Nothing has been pushed. The original REQ-017 report is preserved below.** The original REQ-017 acceptance report is preserved at [`REQ-017-HISTORICAL-FINAL-ACCEPTANCE.md`](REQ-017-HISTORICAL-FINAL-ACCEPTANCE.md).

The owner-approved performance thresholds remain P95 ≤500 ms, P99 ≤1,000 ms and HTTP errors ≤1%. The original stress test **failed** P95 at 564 ms. A later instrumented diagnostic also failed P95 at 638 ms. Neither is represented as passing the 500 ms threshold. Under REQ-020, observed CPU saturation while JMeter, the application and PostgreSQL shared an eight-logical-CPU laptop supports a local `HARDWARE_CONSTRAINED` exception. **Production-scale performance remains UNVERIFIED.**

## Revised gate matrix

| Acceptance gate | Current result | Evidence and qualification |
|---|---|---|
| Java unit tests | PASS — 127, zero failures/errors/skips | [REQ-020 regression log](../verification/REQ-020/quality-regression.log); [Surefire reports](../verification/REQ-020/regression.json) |
| PostgreSQL integration tests and migrations | PASS — 9 tests, six Liquibase changesets per fresh PostgreSQL 16 Testcontainers DB | Same quality log; transaction rollback, CAS, idempotency, outbox and audit immutability cases included |
| JaCoCo line coverage ≥95% | PASS — 97.0195% (944/973) | [Regression record](../verification/REQ-020/regression.json); `target/site/jacoco/jacoco.xml` |
| JaCoCo branch coverage ≥90% | PASS — 90.2326% (582/645) | Same evidence |
| Combined live acceptance scenarios | PASS — 29/29 on actual PostgreSQL-backed running application | [console](../verification/REQ-017-final-live/console.log), [sanitized HTTP exchanges](../verification/REQ-017-final-live/http-exchanges.jsonl), [trace context](../verification/REQ-017-final-live/trace-context.json) |
| Seven JMeter profiles on final source | Six profiles met targets; stress remains historical FAIL at P95 564 ms, P99 767 ms, zero errors | [original comparison](../verification/REQ-017-jmeter-final/comparison.json); original result is unchanged |
| Local performance policy | **PASS — HARDWARE_CONSTRAINED exception under revised local policy; independent review-03 PASS** | [hardware assessment](../verification/REQ-020/hardware-assessment.md); [instrumented samples and analysis](../verification/REQ-020/jmeter-stress-instrumented/). Host CPU peak 99.84%, one-minute load average 28.47 on 8 logical CPUs; same-host JMeter/app/DB CPU sum peaked at 629.7%. The app’s 20-connection pool had up to 178 pending borrowers, disclosed as local capacity pressure. No thermal throttle, RAM exhaustion, material swap paging, excessive GC pause, DB locks/deadlocks, Hikari timeout or outbox backlog was observed. Cgroup CPU throttle counters and per-statement database latency histograms were not captured. |
| Production-scale performance | **UNVERIFIED** | Local exception makes no production-scale claim. |
| Real browser UI | PASS — 17 cases, 155 API requests, zero console errors, 15 screenshots | [browser report](../verification/REQ-017-browser-boundary/run-2026-10-17T060306591Z/browser-e2e.json) |
| Cross-member security | PASS — 7 live PostgreSQL-backed checks | [report](../verification/REQ-017-cross-member/run-20261017T060255912442Z/cross-member.json), sanitized exchanges beside report |
| Postman/Newman | PASS — 34 requests, 14 assertions, zero failures; generated tokens, IDs and keys | [Newman log](../verification/REQ-017-boundary-postman.log) |
| Startup/smoke | PASS | [smoke log](../verification/REQ-017-boundary-smoke-default.log); Compose evidence in REQ-017 verification reports |
| OpenTelemetry and log correlation | PASS with stated run scope | Current 29-case app logs correlate HTTP and async outbox deliveries; collector parent/child spans are verified from a separately identified earlier run. See [trace evidence](../telemetry/REQ-017-trace-verification.json) and [current application log](../verification/REQ-017-final-live/application-trace.log). |
| Requirements C14-01..29 and traceability | PASS for original requirements; REQ-020 accepted with performance exception | [requirement matrix](../verification/REQ-017-requirement-matrix.json), [`docs/testing/traceability-matrix.md`](../../docs/testing/traceability-matrix.md) |
| Editable and rendered architecture diagrams | PASS | [architecture index](../../docs/architecture/README.md) links Mermaid sources and SVG/PNG outputs |
| LLM proof-of-work | **PASS under revised proof-of-work scope; independent review-03 PASS** | [inventory](../audit/REQ-017-session-inventory.json): 67 unique sessions and 9,020 accessible message/tool records; all stored transcript SHA-256 checks, IDs and contained unique transcript paths pass. Ledger: 106 rows, 65 interaction IDs; 64 rows have explicit requirement and task links. Older entries remain less linked. One active coordinator source log changed after export; its transcript digest is stable and valid. |
| LLM raw payload completeness | **PARTIAL — NOT_CAPTURED** | Host-managed raw request/response envelopes, request IDs, system/developer envelopes and encrypted reasoning remain inaccessible. No full-capture claim is made. See [audit summary](../verification/REQ-017-llm-audit.json) and [ledger](../../docs/submission/llm-interactions.jsonl). |
| Workflow validation | PASS — 18 unique dependency-valid tasks | `python3 scripts/workflow/verify_workflow.py`; result retained in [regression record](../verification/REQ-020/regression.json) |
| Independent review of revised acceptance | PASS — review-03 confirmed task and criterion reconciliation | Review requests and responses are preserved under [`reports/sub-agents/T17`](../sub-agents/T17/). |
| Git verification and conditional merge | PASS — local merge commit `18f97aa9c8296fdc107a02192aa4e8e62dd45d78` | `develop` derives from existing `main` `521cb1427f910ebbd01129cc1a950c55b899ee87`; `main` merge parents are `521cb1427f910ebbd01129cc1a950c55b899ee87` and `730ca6fd6569a451a286b189f9dd69893d26c197`. Post-merge audit/evidence commit `d0fd9dd` is recorded on main. No reset or push. |

## Preserved original performance results

The original final-source 500-user stress result remains the original `FAIL`: 88,762 samples, 1,460.5 RPS, P95 564 ms, P99 767 ms, zero errors. The additional 500-user instrumented diagnostic on the same source commit generated 76,251 samples at 1,244.9 RPS, P50 139 ms, P95 638 ms, P99 879 ms, zero errors. Both miss the P95 threshold. Neither report has been changed or relabeled.

The diagnostic observed 99.84% maximum host CPU use and a 28.47 one-minute load average on an eight-logical-CPU laptop while the load generator, claims service, PostgreSQL and other workloads shared the host. It measured ample available RAM (minimum about 12.4 GiB), 27 swap-in pages / zero swap-out pages, no sampled thermal-counter increments, 0.557 seconds of cumulative G1 pauses over about 61 seconds, no database lock waiters/deadlocks, no Hikari timeout, and no outbox pending/failed work. The 20-connection Hikari pool had up to 178 pending borrowers. This supports a benchmark-environment exception for this local laptop, not a production capacity claim. See [`hardware-assessment.md`](../verification/REQ-020/hardware-assessment.md) for all limitations.

## LLM proof-of-work disposition

The latest local export preserves 67 project sessions and 9,020 accessible message/tool records. Transcript hashes match the stored transcript files; session IDs are unique and match their source filenames; transcript paths are unique and contained beneath `reports/audit/transcripts`. The interaction ledger has 106 rows across 65 interaction IDs, including visible task packets, outputs, tool evidence and reviewer outcomes. Explicit requirement/task links exist on 64 rows; remaining historical entries are retained as recorded and are not presented as fully linked. One actively growing coordinator source JSONL changed after the snapshot was taken; the exported transcript itself remains hash-valid. Host-managed raw payloads remain `NOT_CAPTURED`; raw completeness remains `PARTIAL`. The revised PASS is limited to proof that the accessible implementation/review activity occurred.

## Regression and review

On 2026-10-17, `docker run --rm --network host -v /var/run/docker.sock:/var/run/docker.sock -v "$PWD:/workspace" -v /tmp/claims-m2:/root/.m2 -w /workspace maven:3.9.11-eclipse-temurin-21 mvn -B -Pquality clean verify` completed successfully (exit 0): 127 unit tests, 9 PostgreSQL 16/Testcontainers integration tests, six migrations, 97.0195% line coverage and 90.2326% branch coverage. `python3 scripts/workflow/verify_workflow.py` passed with 18 tasks. No application source changed in this policy update.

Independent reviews 01 and 02 found and resolved two process inconsistencies: T13/T16 task state and T13 criterion wording. Final independent review-03 passed the reconciled decision. T13 accepts only the local exception and preserves both numeric failures; T16 proof-of-work passes with raw payloads PARTIAL/NOT_CAPTURED. The acceptance evidence was committed on develop and merged into existing main at `18f97aa9c8296fdc107a02192aa4e8e62dd45d78`; no push occurred.


## Merge and evidence commits

- Accepted evidence commit on `develop`: `45f447022dfbeee6d3d0d4a6609dd31f1b0a31d5`.
- Synchronized checkpoint commit on `develop`: `730ca6fd6569a451a286b189f9dd69893d26c197`.
- Local `develop` → `main` merge commit: `18f97aa9c8296fdc107a02192aa4e8e62dd45d78` (parents: planning baseline `521cb1427f910ebbd01129cc1a950c55b899ee87`; accepted develop `730ca6fd6569a451a286b189f9dd69893d26c197`).
- Post-merge evidence commit on `main`: `d0fd9dd`. A final closeout commit records the evidence commit hash in the current report.
- Push: not performed.

## REQ-021 repository synchronization follow-up

Local `develop` was fast-forwarded to accepted `main`; both local refs matched at `ea89eb3340b0b900f106934aee2a92203c9439e7` during verification. Workflow validation passed with 19 dependency-valid tasks. GitHub branch refs could not be verified from this environment. A non-force SSH push hung and was interrupted; the HTTPS push was rejected by automatic approval review before execution because the remote ancestry could not be verified and explicit push authorization was not established. No remote update is claimed. See [REQ-021 repository evidence](../verification/REQ-021/repository-check.md).

## REQ-022 GitHub synchronization result

The user explicitly authorized normal non-force synchronization. Before push, the remote advertised no branch refs or default HEAD, and fetching `refs/heads/main` returned “couldn't find remote ref”; no remote commits existed to overwrite. An atomic non-force `git push --atomic --progress --porcelain origin develop main` succeeded and created both branches. Post-push fetch and `git ls-remote` verified `origin/main` and `origin/develop` both equal `2cafe2507708c6b773865a04b342ea623248bf02`, the required target. `codex-logs.zip` remained untracked and was not pushed. No force push, reset, deletion, or branch-protection bypass was used. Detailed commands and output are in [REQ-022 remote sync evidence](../verification/REQ-022/remote-sync.md).

The evidence and requirement updates for this push are present in the local worktree, but are intentionally not committed to `main` or `develop`: doing so would violate the explicit requirement that both branch heads remain exactly `2cafe2507708c6b773865a04b342ea623248bf02`.

## REQ-023 — Supplemental Codex archive evidence

A supplied archive was validated and imported additively on 2026-10-17. Its SHA-256 is `8e3e6f75cc7351fb071dd95f513d233c2d94833a4fab7ddd9c14321fafc67664`. It contains 65 ZIP entries (one directory plus 64 project session files): 61 exact duplicates linked to verified existing transcripts, two updated session snapshots and one new session. All 64 transcript links and SHA-256 values verify. The prior 67-session/9,020-record inventory remains preserved as its historical baseline; aggregate record counts were not recomputed across snapshots. Host-managed raw model payloads remain `NOT_CAPTURED`, audit completeness remains `PARTIAL`, and this supplement does not upgrade or alter the application's acceptance decision. The import evidence is in [`REQ-023-codex-archive-import.json`](../audit/REQ-023-codex-archive-import.json); the project acceptance decision remains unchanged. The T20 archive-import task is in review pending an independent review of the importer/evidence.


## REQ-025 — Sanitized archive tracking

The owner selected a sanitized archive copy for Git tracking. `codex-logs.sanitized.zip` SHA-256 is `009d9ae63f7d6d7b94a42508a74dbeae47f6d97d9d89e3c9fbc48dbc583e2738`. It preserves 65 ZIP members (one directory plus 64 JSONL sessions), 28,716 records, record-type histograms, session IDs, and member paths; deterministic regeneration matched the same SHA-256. ZIP CRC and the configured sensitive-pattern scan pass with zero active matches. The original `codex-logs.zip` remains unchanged and untracked. The sanitized copy is selected for the REQ-025 commit and local branch merge; no push is authorized. Host-managed raw model payloads remain `NOT_CAPTURED` and LLM audit completeness remains `PARTIAL`. T20 remains in review until independent review is complete. Git commit/merge evidence will be appended after local integration; no push. See `reports/audit/REQ-025-sanitized-archive.json`.
