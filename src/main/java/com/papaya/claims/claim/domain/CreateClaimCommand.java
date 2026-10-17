package com.papaya.claims.claim.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CreateClaimCommand(BigDecimal policyLimit, String currency, ClaimFacts facts) {
    public CreateClaimCommand {
        Objects.requireNonNull(policyLimit, "policyLimit");
        if (policyLimit.signum() < 0 || policyLimit.scale() > 2
                || policyLimit.precision() - policyLimit.scale() > 16) {
            throw new IllegalArgumentException("Policy limit must be nonnegative and fit numeric(18,2)");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter uppercase code");
        }
        Objects.requireNonNull(facts, "facts");
    }
}
