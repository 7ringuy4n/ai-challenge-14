package com.papaya.claims.claim.domain.persistence;

import java.time.Instant;
import java.util.UUID;

public class OutboxRow {
    public UUID id, claimId; public String effect, payloadJson; public int attempts; public Instant availableAt;
}
