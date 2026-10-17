package com.papaya.claims.claim.domain.persistence;

import java.time.Instant;
import java.util.UUID;

public class AuditRow {
    public UUID id, claimId; public long sequence; public Instant occurredAt; public String fromState, toState,
            actorId, role, reason, correlationId, workflowVersion;
}
