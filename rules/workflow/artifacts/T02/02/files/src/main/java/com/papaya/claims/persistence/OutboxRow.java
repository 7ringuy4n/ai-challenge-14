package com.papaya.claims.persistence;

import java.time.Instant;
import java.util.UUID;

public record OutboxRow(
        UUID id,
        UUID claimId,
        String effect,
        String payload,
        int attempts,
        String status,
        Instant nextAttemptAt) {}
