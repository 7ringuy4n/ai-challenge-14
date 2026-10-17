# T13 independent review — stress-profile remediation experiments

## Scope

Read-only review of three stress-profile runs for commit `0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2`, using the same JMX hash. The reviewer inspected default pool/default Tomcat, temporary pool 40, and temporary pool 20 with Tomcat max threads 500. Raw source data remains in `reports/verification/REQ-017-jmeter-final/stress-20261017T061310Z` and `reports/verification/REQ-017-jmeter-ab/`.

## Reviewer response (conversation-visible, verbatim)

> The results do **not support adopting either remediation**; no code change is justified from these runs.
>
> All three manifests use the same commit and JMeter profile hash, and all runs report zero errors. The initial 20-connection/default-Tomcat run achieved 1,460.5 RPS at P95 564 ms and P99 767 ms. Pool 40 achieved 774.8 RPS at P95 957 ms and P99 1,076 ms. Pool 20 with Tomcat 500 achieved 807.9 RPS at P95 1,046 ms and P99 1,251 ms. Both candidate runs miss the P95 and P99 targets. The pool-40 metrics show 41 database sessions, consistent with 40 app connections plus the monitor.
>
> These are single sequential runs without a documented database reset or run-order controls, so they don’t establish causality. The manifests also omit effective pool and Tomcat settings. Keep the defaults for now; if continuing, repeat interleaved runs from the same database snapshot and record effective settings and Hikari wait metrics. No edits or load runs were performed for this review.

## Coordinator disposition

No source/configuration tuning was adopted. The candidate configurations produced materially worse measurements and are not justified. The default-config repeat was also over target (P95 917 ms, P99 1,077 ms). Record the stress acceptance gate as `FAIL`; the repeated sequential runs are diagnostic only and do not prove a single root cause. A controlled run on a dedicated performance environment with database snapshot/run-order controls is still required. No merge or push.

Raw host-managed model payloads, request IDs, and actual model metadata: `NOT_CAPTURED`; trace audit `PARTIAL`.
