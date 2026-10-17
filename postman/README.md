# Postman collection

`claims-workflow.postman_collection.json` executes the local happy path without manually editing tokens, generated IDs, or request bodies. It obtains CSRF/session state, extracts claim IDs and versions, generates idempotency keys, polls the outbox, closes the claim, and reads the final audit.

Newman 6.2.1 is pinned in `package-lock.json`. `bash scripts/run-postman.sh` sources ignored `.env` into a mode-restricted temporary Newman environment and removes it on exit. Verified on 2026-10-17: 27 requests, 14 assertions, 0 failures. Evidence: [`newman-final.log`](../reports/verification/postman/newman-final.log). Demo credentials are sourced from `.env`; no credential is committed.
