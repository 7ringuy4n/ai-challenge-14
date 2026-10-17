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
