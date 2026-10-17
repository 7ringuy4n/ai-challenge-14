package com.papaya.claims.claim.domain.persistence;

import java.time.Instant;
import java.util.UUID;

public class OutboxRow {
    public UUID id, claimId; public String effect, payloadJson, traceparent; public int attempts; public Instant availableAt;
}
