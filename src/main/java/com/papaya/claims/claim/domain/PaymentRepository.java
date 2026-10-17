package com.papaya.claims.claim.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Durable payment request and trusted confirmation port. */
public interface PaymentRepository {
    void create(UUID claimId, UUID requestId, String currency, BigDecimal amount);
    boolean exists(UUID claimId);
    Optional<Confirmation> confirmation(UUID claimId);
    boolean confirm(UUID claimId, String reference);
    record Confirmation(String reference, java.time.Instant confirmedAt) { }
}
