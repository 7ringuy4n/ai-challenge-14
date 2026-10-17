package com.papaya.claims.claim.domain;

import java.util.Objects;

/** Persistence view that keeps server-owned workflow metadata out of API DTOs. */
public record ClaimAggregate(Claim claim, ClaimWorkflowData workflow) {
    public ClaimAggregate {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(workflow, "workflow");
    }
}
