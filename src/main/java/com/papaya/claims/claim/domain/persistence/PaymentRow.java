package com.papaya.claims.claim.domain.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentRow {
    public UUID id, claimId, requestId; public String currency, reference; public BigDecimal amount; public Instant confirmedAt;
}
