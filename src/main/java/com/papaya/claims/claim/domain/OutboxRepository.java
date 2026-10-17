package com.papaya.claims.claim.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Durable at-least-once outbox. Dispatch consumers must tolerate retries. */
public interface OutboxRepository {
    void enqueue(Message message);
    List<Message> claimDue(Instant now, int limit);
    boolean markDelivered(UUID id, Instant deliveredAt);
    boolean markFailed(UUID id, String safeError, Instant retryAt);
    record Message(UUID id, UUID claimId, String effect, String payload, int attempts, Instant availableAt, String traceparent) {
        public Message(UUID id, UUID claimId, String effect, String payload, int attempts, Instant availableAt) {
            this(id, claimId, effect, payload, attempts, availableAt, null);
        }
    }
}
