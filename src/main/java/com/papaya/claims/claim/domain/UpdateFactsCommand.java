package com.papaya.claims.claim.domain;

import java.util.Objects;

public record UpdateFactsCommand(long expectedVersion, ClaimFacts facts) {
    public UpdateFactsCommand {
        if (expectedVersion < 0) throw new IllegalArgumentException("Expected version cannot be negative");
        Objects.requireNonNull(facts, "facts");
    }
}
