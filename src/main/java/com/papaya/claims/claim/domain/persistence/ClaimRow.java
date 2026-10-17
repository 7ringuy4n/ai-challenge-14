package com.papaya.claims.claim.domain.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ClaimRow {
    public UUID id; public String memberId; public String state; public BigDecimal policyLimit; public String currency;
    public long version; public String factsJson; public int informationRequests;
    public Instant lastInformationRequestedAt, lastInformationReceivedAt, assessmentStartedAt, rejectedAt,
            memberAcknowledgedRejectionAt, createdAt, updatedAt;
}
