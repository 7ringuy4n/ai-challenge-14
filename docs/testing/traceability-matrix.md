# Requirement traceability matrix — REQ-20261017-017

Live evidence: [`acceptance-matrix/console.log`](../verification/acceptance-matrix/console.log) (29 checks) and [`http-exchanges.jsonl`](../verification/acceptance-matrix/http-exchanges.jsonl) (31 redacted HTTP exchanges). The console and exchange evidence is from the combined live run on 2026-10-17; browser and PostgreSQL-backed two-member evidence is under `reports/verification/REQ-017-browser/` and `reports/verification/REQ-017-cross-member/`.

| ID | Acceptance requirement | Implementation/test evidence | Combined live run |
|---|---|---|---|
| C14-01 | Workflow config has 8 states/9 edges and role, guard, effect metadata | `workflow.yml; ResourceContractTest; WorkflowConfigTest` | HTTP transitions/options case 25; API boundary |
| C14-02 | Five-event success path closes | `ClaimServiceTest; ClaimPersistenceIT` | cases 9-20: complete approved/payment path, audit, close |
| C14-03 | Rejection reason, appeal instructions, system closure | `ClaimServiceTest; workflow.yml; browser-e2e.mjs` | Browser case 17 rejects and acknowledges a claim; API acceptance cases test the approved close path |
| C14-04 | Information loop resets timer and stale completion | `ClaimServiceTest; workflow.yml` | Focused unit evidence |
| C14-05 | Illegal edge reports source/target/options | `WorkflowEngineTest; GlobalExceptionHandlerTest` | case 21 terminal illegal transition; exact detail via captured JSON |
| C14-06 | Unauthorized role no mutation/audit/outbox | `ClaimServiceTest; SecurityConfigurationTest` | cases 8,16,26 |
| C14-07 | Missing/invalid docs fail with guard detail | `GuardEvaluatorTest; ClaimServiceTest` | Focused unit evidence |
| C14-08 | Assessment requires assigned assessor | `ClaimServiceTest; GuardEvaluatorTest` | Focused unit evidence |
| C14-09 | Incomplete assessment cannot decide | `ClaimServiceTest; GuardEvaluatorTest` | Focused unit evidence |
| C14-10 | Amount/currency boundaries | `CreateClaimRequestValidationTest; ClaimServiceTest` | case 28 invalid currency; unit boundary suite |
| C14-11 | Reason/description required | `TransitionRequest validation; ClaimServiceTest` | Focused unit/API validation evidence |
| C14-12 | Fresh information receipt required | `ClaimServiceTest; ClaimPersistenceIT` | Focused unit/PG evidence |
| C14-13 | Persisted payment/confirmed reference required | `PaymentOutboxIT; ClaimServiceTest` | cases 14-18 live payment + close |
| C14-14 | Appeal time/ack deterministic | `ClaimServiceTest` | Focused unit evidence |
| C14-15 | 3 cycles and exact fourth escalation; claim independence | `ClaimServiceTest; workflow.yml` | Focused unit evidence |
| C14-16 | Audit fields/order/scope | `ClaimPersistenceIT; audit API` | cases 6,20; HTTP transcript |
| C14-17 | DB audit append-only and no cascade | `AuditImmutabilityIT` | PostgreSQL/Testcontainers evidence |
| C14-18 | Failure rolls back state/audit/counter/payment | `TransactionRollbackIT` | PostgreSQL/Testcontainers evidence |
| C14-19 | Rollback dispatch none; after-commit retry | `OutboxDispatcherTest; PaymentOutboxIT` | PostgreSQL outbox tests; live payment in cases 14-18 |
| C14-20 | Same version one concurrent winner | `ConcurrencyIT` | PostgreSQL/Testcontainers evidence |
| C14-21 | Idempotent same payload replay; changed payload conflict | `IdempotencyIT` | cases 3-4,24 |
| C14-22 | Config-only workflow extension/dynamic options | `WorkflowEngineTest; ResourceContractTest` | case 25 confirms dynamic options; config extension is unit-only |
| C14-23 | Invalid workflow config fails clearly | `WorkflowConfigTest` | Focused unit/startup contract evidence |
| C14-24 | Terminal state has no outgoing choices | `WorkflowEngineTest` | case 25 |
| C14-25 | HTTP auth/ownership/system spoof | `SecurityConfigurationTest; ClaimServiceTest; verify-cross-member.py` | cases 1,5,16,26 plus 7 live two-member HTTP checks and unchanged PostgreSQL state |
| C14-26 | Malformed UUID/missing claim/oversized/invalid input | `GlobalExceptionHandlerTest; JsonRequestSizeFilterTest` | cases 22-23,27-28 |
| C14-27 | UI dynamic states/options/audit safe render | `static UI code; browser-e2e.mjs` | Real Chrome CDP 17/17 checks; screenshots, console and request/status evidence saved per run |
| C14-28 | Compose fresh/repeated setup, durable DB/runtime credentials | `Compose setup + PostgreSQL tests` | compose logs and live run evidence |
| C14-29 | CSRF invalid/missing/valid; no secret logs | `SecurityConfigurationTest; CorrelationIdFilterTest` | cases 2,7,9-18; HTTP transcript |

## Coverage gaps

- C14-25 cross-member detail/audit/facts/transition checks and database invariants passed against two configured member principals; the local default remains `member-demo`.
- C14-01, C14-04, C14-07–12, C14-14–15, C14-22–23 rely on focused unit/config tests rather than live HTTP scenarios; the 29-case runner groups boundary coverage but is not evidence that every internal acceptance criterion was separately exercised through HTTP.
- Full original and subsequent requirement links remain in `rules/REQUIREMENT.md`; detailed quality-case mapping is separately maintained here.

The traceability evidence does not constitute project acceptance. Final-source seven-profile JMeter, exhaustive session audit, and the mandatory complete host-managed raw model-payload capture gate remain outstanding until their separate reports say otherwise.
