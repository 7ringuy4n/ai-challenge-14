# T13 read-only pool-size assessment

## Request (conversation-visible)

Independent read-only review request for a potential performance remediation. The final-source JMeter stress profile (500 threads, 60s) on commit 0435e1b has 88,762 samples, P95 564ms (target 500), P99 767ms, zero errors. Per-sampler elapsed P95: GET session 201ms, create claim 630ms (500 requests), list claims 662ms. Peak app CPU ~280%, PostgreSQL ~244%, active DB connections 21 (pool default 20). The default app Hikari pool is 20; PostgreSQL default max_connections likely 100. Assess whether raising default/configured Hikari pool to 40 is a justified safe remediation; inspect pool/compose/mapper/schema and advise risks/tests, without editing. Preserve review response in handoff; no need to use subagents.

## Reviewer response (conversation-visible, verbatim)

> Assessment: do not raise the default pool to 40 based on this evidence alone. A 40-connection value is a reasonable controlled A/B experiment for a single app against stock PostgreSQL `max_connections=100`, with connection headroom checked and per-replica budgeting; it is not yet a justified remediation.
>
> Evidence/risks:
> - `application.yml:8-9` already exposes `CLAIMS_DB_POOL_SIZE`, default 20. `docker/compose.yml` does not pass this variable to the app container, so setting it in the invoking shell or `.env` will not actually change the container pool unless Compose forwards it (or an override config is used).
> - Recorded 21 `active_connections` does not establish pool saturation: `scripts/performance/monitor.py` counts all `pg_stat_activity` rows without distinguishing active from idle and its own `psql` probe adds a connection. It captures no Hikari active/idle/pending/acquisition/timeout metrics.
> - App CPU ~280% and PostgreSQL ~244% indicate meaningful CPU load. A larger pool may reduce connection-queue wait if Hikari is saturated, but can increase concurrent query CPU and lock contention. `GET /api/v1/session` is not a database operation (`SessionController.session`); its 201ms P95 points to authentication/application CPU as another contributor. It cannot be fixed by a larger DB pool alone. Query cost also matters: list uses page plus count (`ClaimMapper.xml:28-31`) within a `REPEATABLE_READ` transaction.
>
> Recommendation: keep 20 as default; perform a same-commit, same-data A/B at 20 vs 40 after forwarding an explicit Compose override. Capture Hikari pool active/idle/pending/acquisition latency/timeouts; PostgreSQL active/wait-event counts and CPU; app CPU; per-sampler P95/P99/error rate. Include stress and concurrent-claims, plus a normal/soak stability check. Accept 40 only if P95 improvement is repeatable and PG CPU/locks, errors, memory, and connection headroom remain healthy. Default PostgreSQL max_connections is normally 100 for this official image, but confirm the running value and size total pool across replicas, monitors, migrations, and admin usage. No files edited/tests/load run. Host-managed raw payloads: `NOT_CAPTURED/PARTIAL`.

## Capture limits

Host-managed dispatch payload, response envelope, request/response IDs, actual runtime model metadata, token usage, and latency were not exposed by the collaboration host; mark `NOT_CAPTURED`, overall trace `PARTIAL`.
