package com.papaya.claims.claim.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Safe operational audit metadata; request content and secrets are intentionally excluded. */
public record SystemOperationAuditRecord(UUID id, String action, String resource, String resourceId,
        String actorId, String correlationId, String outcome, String errorType, Instant occurredAt) {
    public SystemOperationAuditRecord {
        Objects.requireNonNull(id, "id");
        require(action, "action"); require(resource, "resource"); require(actorId, "actorId");
        Objects.requireNonNull(correlationId, "correlationId"); require(outcome, "outcome");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
