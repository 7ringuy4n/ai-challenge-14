package com.papaya.claims.claim.api.dto;

import com.papaya.claims.claim.domain.AuditRecord;
import java.time.Instant;
import java.util.UUID;

public record AuditEntryView(UUID id, long sequence, Instant timestamp, String fromState, String toState,
                             String actorId, String role, String reason, String correlationId) {
    public static AuditEntryView from(AuditRecord record) {
        return new AuditEntryView(record.id(), record.sequence(), record.occurredAt(), record.fromState(),
                record.toState(), record.actorId(), record.role(), record.reason(), record.correlationId());
    }
}
