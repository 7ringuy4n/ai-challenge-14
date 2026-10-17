# Independent review 01 — initial decision review

## Request

# T17 independent acceptance decision review

**Review type:** independent, read-only evidence and acceptance-policy review. Do not edit files, run tests, commit, merge, or accept the project.
**Requirement:** REQ-20261017-020.
**Source under review:** application commit `0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2`; this change is documentation/evidence-only.

## Decision questions

1. Do original and instrumented stress outcomes remain accurately reported as FAIL against P95 ≤500 ms, without overwriting the historical reports or implying production-scale performance passed?
2. Do the captured host/JMeter/app/PostgreSQL/GC/connection/lock/outbox measurements reasonably support `HARDWARE_CONSTRAINED` for this shared local laptop under the owner-approved policy, with limitations such as uncaptured cgroup counters and no per-statement latency histogram disclosed? Is there any material application defect evidence that should block the exception?
3. Does the refreshed transcript export provide proof-of-work under the revised policy: unique session IDs, contained unique transcript paths, transcript SHA-256 integrity, accessible user/assistant and tool evidence, and ledger traceability where available? Is the active-source-file hash qualification honest? Are inaccessible host envelopes properly `NOT_CAPTURED`, with raw completeness `PARTIAL`?
4. Did the complete 127 unit + 9 PostgreSQL integration regression, all 6 changesets, JaCoCo 97.0195% lines / 90.2326% branches, and workflow validation actually pass as reported?
5. Are any revised mandatory gates still unresolved such that marking `ACCEPTED_WITH_PERFORMANCE_EXCEPTION` or merging `develop` into `main` would be unsafe or unsupported?

## Evidence to inspect

- `reports/verification/REQ-020/hardware-assessment.md`
- `reports/verification/REQ-020/jmeter-stress-instrumented/resource-analysis.json` and raw `resource-metrics.jsonl`
- `reports/verification/REQ-017-jmeter-final/stress-20261017T061310Z/summary.json` (original failure; do not modify)
- `reports/verification/REQ-020/regression.json`, `quality-regression.log`, `target/surefire-reports/`, `target/failsafe-reports/`, `target/site/jacoco/jacoco.xml`
- `reports/audit/REQ-017-session-inventory.json`, `reports/audit/transcripts/`, `docs/submission/llm-interactions.jsonl`, `reports/verification/REQ-017-llm-audit.json`
- `reports/verification/REQ-017-current.json`, `reports/verification/REQ-017-requirement-matrix.json`, `reports/acceptance/FINAL-ACCEPTANCE.md`, and archived `reports/acceptance/REQ-017-HISTORICAL-FINAL-ACCEPTANCE.md`
- `rules/REQUIREMENT.md`, `rules/workflow/tasks.json`, current Git refs/history

Return verdict `PASS`, `REWORK`, or `BLOCKED`. List critical/high findings first with file paths and evidence. Distinguish reviewer judgment from measured results. Explicitly state whether the local exception can pass under the policy, whether LLM proof-of-work passes while raw payload status remains PARTIAL, and whether conditional local merge is justified after this review. No production performance claim. Preserve your exact visible task packet and response in `reports/sub-agents/T17/review-01.md`; raw host payload IDs remain `NOT_CAPTURED`.

## Response (visible output, verbatim)

**Verdict: REWORK**

**High finding — task state does not yet support an unambiguous acceptance decision.** In `rules/workflow/tasks.json`, T17 is running and depends on T13 and T16, which are also still marked `running`. The evidence supports the revised dispositions—stress remains a failure classified under the local hardware exception, and accessible LLM proof-of-work passes with raw completeness `PARTIAL`—but those task states have not been reconciled with them. Update the task and acceptance records so they state which gates pass under REQ-020 and which historical results remain failures before marking the project accepted or merging. The current `reports/acceptance/FINAL-ACCEPTANCE.md` correctly remains pending review.

**Measured results:** The original stress summary reports 88,762 samples, P95 564 ms, P99 767 ms and zero errors; the instrumented run reports 76,251 samples, P95 638 ms, P99 879 ms and zero errors. Both are correctly identified as failures against P95 ≤500 ms. The hardware evidence reasonably supports `HARDWARE_CONSTRAINED` for this shared laptop under the revised local policy: the samples show host CPU saturation while JMeter, the application and PostgreSQL ran together. The uncaptured cgroup throttling counters and per-statement latency histogram are disclosed, as are the Hikari pending borrowers. I found no material application defect evidence in the reviewed measurements that would block the local exception. Production-scale performance remains unverified.

**Audit evidence:** I independently checked all 65 inventory entries: session IDs and transcript paths are unique; transcript paths resolve under `reports/audit/transcripts`; every stored transcript SHA-256 matches; and the exports contain accessible user/assistant and tool-call/output records. The inventory and export contain 8,758 records. The active source-file hash qualification is explicit, and host-managed envelopes and IDs remain `NOT_CAPTURED`; raw completeness is honestly `PARTIAL`. Proof-of-work passes under the revised policy.

**Regression evidence:** The retained log reports 127 unit tests and 9 integration tests with no failures, errors or skips, followed by `BUILD SUCCESS`. It records all six changesets. The JaCoCo XML reports 944/973 lines and 582/645 branches, matching 97.0195% and 90.2326%. The retained workflow validation says PASS for 18 dependency-valid tasks. These checks pass as reported; I did not rerun them.

The local hardware exception and LLM proof-of-work are supported. After reconciling the T13/T16 task states and recording this review, a conditional local merge is justified if the coordinator confirms the revised gates and commits the evidence to `develop`; the current evidence changes are uncommitted. No production performance claim is justified. No files were edited and no tests were run.
