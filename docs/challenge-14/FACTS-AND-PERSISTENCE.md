# Workflow fact and trusted evidence contract

Contract addendum: **CH14-Contracts-v1 / AD-01**
Origin: T01/05 handoff (`LLM-20261017-009-T01-WORKER-05`) and T02/07 review-01 (`LLM-20261017-010-T02-REVIEW-01`).
Status: coordinator decision from explicit preconditions in `requirements/AI_Challenge_14.md`; no transition, role, error text, or cycle limit is changed. The initial version's document-merge ambiguity was found in `LLM-20261017-010-T00-REVIEW-AD01` and resolved below.

Service/API extension **AD-02** (2026-10-17): the explicit member acknowledgment required by AD-01 is a separate authenticated, idempotent action on a rejected claim; it increments the claim version and stamps only server time. The API also exposes configured available transition choices so a client can render current role/guard status. This does not add a workflow edge or permit client writes to trusted evidence. Origin: implementation gap found while continuing REQ-20261017-010; it follows the pre-existing AD-01 evidence contract and user-authorized goal to complete the workflow.

## Claim facts

`ClaimFacts` contains business data from authenticated claim actors. Before evaluation, the service validates the submitted patch shape, authorizes every changed field against the authenticated actor and current claim, then merges it with persisted facts and writes the merged facts atomically with a successful claim mutation. Other top-level facts use shallow replacement (a supplied value replaces that top-level value); the `documents` map uses the field-level merge rule below. Missing, malformed, wrongly typed, or unauthorized fields fail closed without mutation.

| Fact key | Shape and guard use | Who may assert or change it |
|---|---|---|
| `documents` | Non-empty object keyed by stable document ID (`[A-Za-z0-9][A-Za-z0-9._:-]{0,119}`), with boolean `required`, `present`, and `valid` fields per entry. At least one entry must be required; every required document must be present and valid. | `document_clerk` creates roster entries and sets `required`, `valid`, and `present`. The claim-owning member can update only `present` on an existing document ID. Entries are never deleted. |
| `assessorId` | Nonblank string. | `team_lead`. |
| `assessmentReport` | Object with boolean `complete`; only literal `true` passes. | `assessor`. |
| `requestedAmount` | Exact decimal JSON number, nonnegative, no more than `Claim.policyLimit`. Currency is the claim's immutable currency. | Set at claim creation; immutable afterward. |

The transition `reason` is the rejection reason for `rejection_reason_present` and the missing-information description for `information_description_present`; it is also the user-supplied audit note. Both guards require nonblank text. `TransitionCommand.suppliedFacts` cannot set server-owned evidence fields.

Document updates merge by document ID and then by field: an authorized partial update to `present` preserves the existing `required` and `valid` values. Omitted document IDs and omitted fields preserve their stored values. Null document entries or null fields are rejected; there is no delete operation for a roster entry. Members cannot add, remove, rename, or modify document roster entries, and cannot send `required` or `valid`; such attempts are rejected. Clerks may add entries while the claim is `SUBMITTED` or `PENDING_INFO`, including newly requested documents; existing entries are never removed or renamed. The `required` flag on an existing entry can be changed only while the claim is `SUBMITTED`; it is immutable afterward. A clerk may change `present` or `valid` while the claim is `SUBMITTED` or `PENDING_INFO`. No list-index identity or recursive merge is used.

## Server-owned workflow evidence

The service constructs an immutable `WorkflowEvidence` from database state; it is never deserialized from request facts. Timestamps are truncated to PostgreSQL microsecond precision when written and compared at that precision.

| Evidence | Source and update rule |
|---|---|
| `informationRequests`, `lastInformationRequestedAt` | Claim row. Increment and stamp atomically when the configured cycle-policy transition succeeds. The configured maximum remains three; attempt four returns the exact configured message. |
| `lastInformationReceivedAt` | Claim row. Service stamps it from injected `Clock` when authenticated member-owned new information is persisted while the claim is `PENDING_INFO`; request timestamps are ignored. |
| `assessmentStartedAt` | Claim row. Service stamps it when `record_assessment_start` succeeds. |
| `rejectedAt` | Claim row. Service stamps it in the successful `UNDER_ASSESSMENT -> REJECTED` transaction. |
| `memberAcknowledgedRejectionAt` | Claim row. Service stamps it only for the authenticated owner member's explicit acknowledgement while the claim is `REJECTED`; a request boolean is an action signal, never a timestamp. |
| payment-request/confirmation evidence | `payment_requests` row. Only the service/mock payment adapter writes a request, confirmation timestamp, and reference. Client facts cannot assert a request or confirmation. |

The engine receives `WorkflowEvaluationContext(Claim claim, Actor actor, TransitionCommand command, WorkflowEvidence evidence, Instant now, Duration appealPeriod)`. It is pure: it reads the persisted claim, the already-authorized business-fact merge, and server-built evidence; it performs no I/O or writes.

## Guard semantics

- `all_required_documents_present`: validate the `documents` shape and the required-document rule above.
- `assessor_assigned`: `assessorId` is a nonblank string.
- `assessment_report_complete`: `assessmentReport.complete` is exactly `true`.
- `amount_within_policy_limit`: `requestedAmount` is an exact decimal, nonnegative and at most the claim policy limit.
- `rejection_reason_present` and `information_description_present`: transition reason is nonblank.
- `new_information_received`: server-stamped `lastInformationReceivedAt` is strictly after `lastInformationRequestedAt`.
- `payment_request_exists`: a persisted payment request exists for the claim.
- `payment_confirmed`: the persisted payment request has a nonblank reference and server-stamped confirmation time.
- `appeal_period_expired_or_member_acknowledged`: either `now >= rejectedAt + appealPeriod`, or an owner-member acknowledgement timestamp is at/after `rejectedAt`.

The appeal duration is `claims.workflow.appeal-period`, environment override `CLAIMS_APPEAL_PERIOD`, default `P30D`. The challenge specifies expiry behavior but no duration; 30 days is the documented configurable local default, not a claimed external policy.

The engine resolves configured guard/effect IDs through strategy registries; it does not branch on state names. A configured cycle policy selects the transition ID, counter and exact error message. Duplicate state/transition IDs, duplicate `(from,to)` pairs, dangling states, unknown roles/strategies and malformed configuration fail startup. A state/edge extension using existing roles, guards and effects remains configuration-only.

## Persistence/API seams

- Typed `ClaimAggregate` combines the public `Claim` value with `ClaimWorkflowData`; API DTOs do not expose server metadata.
- `ClaimWorkflowData` stores the cycle counter and trusted timestamps in the claim row. Payment evidence stays in `payment_requests`.
- `ClaimRepository` compare-and-set writes the claim and workflow data under one transaction/version check. Domain audit, idempotency receipt, payment, system audit and outbox writes share the same transaction.
- Runtime grants are explicit by table. `claims_runtime` cannot UPDATE or DELETE either audit table and cannot alter immutable identifiers/payload columns in payment/outbox rows. No broad `ALTER DEFAULT PRIVILEGES` grants are used.
- PostgreSQL Testcontainers must create distinct migration/runtime roles and verify actual grants, rollback, concurrency and exact JSON numeric behavior. Static source inspection is not database evidence.
