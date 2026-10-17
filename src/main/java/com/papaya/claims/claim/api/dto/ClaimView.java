package com.papaya.claims.claim.api.dto;

import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimFacts;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ClaimView(UUID id, String state, BigDecimal policyLimit, String currency,
                        long version, Map<String, Object> facts,
                        Instant createdAt, Instant updatedAt) {
    public ClaimView {
        Objects.requireNonNull(id, "id");
        if (state == null || state.isBlank()) throw new IllegalArgumentException("Claim state is required");
        Objects.requireNonNull(policyLimit, "policyLimit");
        if (policyLimit.signum() < 0 || policyLimit.scale() > 2
                || policyLimit.precision() - policyLimit.scale() > 16) {
            throw new IllegalArgumentException("Policy limit must be nonnegative and fit numeric(18,2)");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Currency is invalid");
        if (version < 0) throw new IllegalArgumentException("Claim version cannot be negative");
        facts = new ClaimFacts(Objects.requireNonNull(facts, "facts")).values();
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static ClaimView from(Claim claim) {
        return new ClaimView(claim.id(), claim.state(), claim.policyLimit(), claim.currency(), claim.version(), claim.facts().values(),
                claim.createdAt(), claim.updatedAt());
    }
}
