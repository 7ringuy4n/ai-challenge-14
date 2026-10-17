package com.papaya.claims.claim.domain.persistence;

import java.time.Instant;
import java.util.UUID;

public class SystemAuditRow {
    public UUID id; public String action, resource, resourceId, actorId, correlationId, outcome, errorType;
    public Instant occurredAt;
}
