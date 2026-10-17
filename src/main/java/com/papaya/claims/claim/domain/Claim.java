package com.papaya.claims.claim.domain;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record Claim(UUID id, String memberId, String state, BigDecimal policyLimit, String currency,
                    long version, ClaimFacts facts,
                    Instant createdAt, Instant updatedAt) {
    public Claim {
        Objects.requireNonNull(id, "id");
        if (memberId == null || memberId.isBlank()) throw new IllegalArgumentException("Member id is required");
        if (state == null || state.isBlank()) throw new IllegalArgumentException("Claim state is required");
        Objects.requireNonNull(policyLimit, "policyLimit");
        if (policyLimit.signum() < 0 || policyLimit.scale() > 2
                || policyLimit.precision() - policyLimit.scale() > 16) {
            throw new IllegalArgumentException("Policy limit must be nonnegative and fit numeric(18,2)");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter uppercase code");
        }
        if (version < 0) throw new IllegalArgumentException("Claim version cannot be negative");
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
