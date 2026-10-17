# REQ-020 local performance environment assessment

**Assessment status: `HARDWARE_CONSTRAINED` is accepted for local benchmark acceptance after independent review-03 PASS. Production-scale performance remains unverified.** This assessment does not change any measured result or claim that the P95 500 ms target passed.

## Preserved results

- The original final-source stress run remains unchanged at [`REQ-017-jmeter-final/stress-20261017T061310Z`](../REQ-017-jmeter-final/stress-20261017T061310Z): 500 VUs, 88,762 samples, 1,460.5 RPS, P95 564 ms, P99 767 ms, zero errors. Its original result is `FAIL` against P95 ≤500 ms.
- An additional monitored stress diagnostic ran against the same clean application source commit `0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2`, with the same 500 VUs and unchanged profile hash. It produced 76,251 samples, 1,244.9 RPS, P50 139 ms, P95 638 ms, P99 879 ms, zero errors. It also fails the original P95 threshold. The run is separate and did not overwrite the original evidence.
- Command: `bash scripts/performance/run-profile.sh stress`, from `/tmp/claims-perf-0435e1b`, with [the REQ-020 monitor](performance-monitor.py) sampling for 150 seconds. Run console, manifest, JTL, summary, JMeter log, HTML dashboard, resource samples and derived analysis are in [`jmeter-stress-instrumented`](jmeter-stress-instrumented/).

## Environment signals during the monitored run

| Signal | Observation | Interpretation |
|---|---:|---|
| Host CPU | 11th Gen Intel Core i5-11320H, 4 physical cores / 8 logical CPUs | A small laptop CPU ran load generator, service, database, collector and other desktop/container workloads on one host. |
| Total host CPU use | Peak 99.84%; load average peaked at 28.47 during the run | Direct evidence of host CPU saturation and runnable work far exceeding the eight logical CPUs. |
| JMeter process | Peak 276% CPU; peak RSS about 997 MiB | The load generator itself consumed roughly 2.76 logical CPUs and substantial memory on the same saturated host. |
| Claims app container | Peak 282.5% CPU; peak memory about 1.25 GiB | The target service competed for the same finite host CPU. |
| PostgreSQL container | Peak 217.18% CPU; peak memory about 103 MiB | The database also consumed substantial host CPU while the test ran. |
| Sampled CPU thermal-throttle counters | No counter increments during the monitor window | No active thermal-throttling event was observed in this run. Historical cumulative counter values are not attributed to the run. |
| Host RAM | Minimum available RAM was about 12.4 GiB | Memory capacity was not exhausted. |
| Swap | At most about 809 MiB was allocated; 27 swap-in pages and zero swap-out pages across the full monitor window | No material swap activity was observed during this sample. The small page count is retained rather than described as zero. |
| JVM GC | 30 additional G1 young collections and about 0.557 seconds cumulative pause time in the stress interval | GC pause time was under one percent of the roughly 61-second load interval and does not explain the latency miss. |
| Hikari pool | 20 active connections maximum, up to 178 pending borrowers, zero connection timeouts | The application queued concurrent database work while the host was saturated. This is a capacity signal and is disclosed; it is not hidden or treated as proof of production capacity. |
| PostgreSQL waits | At most 21 active connections including monitor; no lock waiters or deadlocks; sampled max active-query age 0.271 s | No lock-contention or deadlock defect was observed. Per-statement latency histograms were not enabled. |
| Outbox | No pending or failed outbox work in sampled intervals | No outbox backlog explains the response-time miss. |
| Container CPU quota | No explicit CPU limit is configured in the Compose file; live cgroup throttling counters were not retained | Host CPU saturation is measured; a cgroup quota contribution was not independently measured. |

Full samples, including timestamps and sampled metric values, are retained in [`resource-metrics.jsonl`](jmeter-stress-instrumented/resource-metrics.jsonl). The derived numbers and qualifications are in [`resource-analysis.json`](jmeter-stress-instrumented/resource-analysis.json). The monitor records aggregate resource data only; it does not retain credentials, claim bodies or SQL text.

## Decision basis and limits

The test’s own load generator used multiple CPUs at the same time the app and PostgreSQL were active, and total host CPU reached 99.84% with a load average of 28.47 on eight logical CPUs. This is direct evidence that this local benchmark environment was CPU-constrained during the stress profile. RAM pressure, active thermal throttling, long GC pauses, database locks, connection timeouts and outbox backlog were not observed. The Hikari pending queue is a real local capacity signal, but no matching database lock wait or timeout was observed; under the fully CPU-saturated colocated test it does not establish a code defect or production pool requirement.

Accordingly, the owner-approved local-environment policy supports a `HARDWARE_CONSTRAINED` exception for this laptop benchmark, subject to independent review and the applicable regression run. The measured original and monitored P95 results remain FAIL against 500 ms. The exception does not establish production-scale latency or throughput and does not replace a production-sized benchmark.
