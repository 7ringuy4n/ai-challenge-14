package com.papaya.claims.claim.domain;

import java.util.Objects;

public record TransitionResult(Claim claim, AuditRecord audit, boolean replayed) {
    public TransitionResult {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(audit, "audit");
    }
}
