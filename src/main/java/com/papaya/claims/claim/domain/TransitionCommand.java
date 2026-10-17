package com.papaya.claims.claim.domain;

import java.util.Objects;

public record TransitionCommand(String targetState, long expectedVersion, String reason, ClaimFacts suppliedFacts) {
    public TransitionCommand {
        if (targetState == null || targetState.isBlank()) throw new IllegalArgumentException("Target state is required");
        if (expectedVersion < 0) throw new IllegalArgumentException("Expected version cannot be negative");
        reason = Objects.requireNonNullElse(reason, "");
        suppliedFacts = Objects.requireNonNullElseGet(suppliedFacts, () -> new ClaimFacts(java.util.Map.of()));
    }
}
