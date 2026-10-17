package com.papaya.claims.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimRow(
        UUID id,
        String memberId,
        String state,
        BigDecimal policyLimit,
        String currency,
        String facts,
        int informationRequests,
        long version,
        Instant assessmentStartedAt,
        Instant lastInformationRequestedAt) {}
