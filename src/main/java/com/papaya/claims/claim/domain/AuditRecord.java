package com.papaya.claims.claim.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuditRecord(UUID id, UUID claimId, long sequence, Instant occurredAt, String fromState,
                          String toState, String actorId, String role, String reason,
                          String correlationId, String workflowVersion) {
    public AuditRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(claimId, "claimId");
        if (sequence < 0) throw new IllegalArgumentException("Audit sequence cannot be negative");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (toState == null || toState.isBlank()) throw new IllegalArgumentException("Audit target state is required");
        if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("Audit actor is required");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(workflowVersion, "workflowVersion");
    }
}
