package com.papaya.claims.claim.service;

import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.AuditRecord;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.CreateClaimCommand;
import com.papaya.claims.claim.domain.PageRequest;
import com.papaya.claims.claim.domain.PageResult;
import com.papaya.claims.claim.domain.TransitionCommand;
import com.papaya.claims.claim.domain.TransitionResult;
import com.papaya.claims.claim.domain.UpdateFactsCommand;
import java.util.UUID;
import java.util.List;
import com.papaya.claims.workflow.WorkflowOption;

/** Stable service boundary consumed by REST adapters. */
public interface ClaimUseCases {
    Claim create(CreateClaimCommand command, Actor actor, String idempotencyKey, String correlationId);
    Claim get(UUID claimId, Actor actor);
    List<WorkflowOption> options(UUID claimId, Actor actor);
    PageResult<Claim> listVisible(PageRequest page, Actor actor);
    Claim updateFacts(UUID claimId, UpdateFactsCommand command, Actor actor,
                      String idempotencyKey, String correlationId);
    Claim acknowledgeRejection(UUID claimId, long expectedVersion, Actor actor,
                               String idempotencyKey, String correlationId);
    TransitionResult transition(UUID claimId, TransitionCommand command, Actor actor,
                                String idempotencyKey, String correlationId);
    TransitionResult systemTransition(UUID claimId, TransitionCommand command, Actor actor,
                                      String idempotencyKey, String correlationId);
    PageResult<AuditRecord> audit(UUID claimId, PageRequest page, Actor actor);
}
