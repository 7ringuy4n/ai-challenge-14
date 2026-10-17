# Acceptance scenario runner

The scenario runner exercises the already running application over HTTP. It uses a separate cookie jar and CSRF token for each authenticated client, reads `.env` as data, and accepts environment overrides for `HOST_PORT`, `DEMO_PASSWORD`, `SYSTEM_PASSWORD`, `BASE_URL`, `ENV_FILE`, and `SCENARIO_OUTPUT`. It never writes credentials, authorization headers, cookies, or CSRF tokens to scenario output.

Start the app separately with `bash scripts/setup.sh`, then `docker compose --env-file .env -f docker/compose.yml up -d --build --wait`. Run `bash scripts/smoke.sh` and then `bash scripts/run-scenarios.sh`. Exports are written to `reports/verification/scenarios/`, one JSON file for each of 14 cases. Each export contains the actual claim response, actual audit response, and the asserted outcome. Failed HTTP assertions also include the safe error envelope and verify that the audit list did not change.

The cases cover the five transition happy path, rejection and protected system close, an eight-transition information loop followed by approval/payment/close, invalid transition, wrong role, missing-document guard, the fourth information request and its exact limit message, stale version, idempotent replay, cross-member read denial, actor-body spoofing, system/human endpoint boundaries, missing CSRF, and unauthenticated access.

All checks remain `NOT_RUN` until the coordinator runs them. Store the actual command, exit code, source hash, test counts, measured coverage, and export paths under `reports/`. The script makes no performance or capacity claim.
