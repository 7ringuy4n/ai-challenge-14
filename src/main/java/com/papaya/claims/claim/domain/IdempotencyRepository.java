package com.papaya.claims.claim.domain;

import java.util.Optional;
import java.util.UUID;

/** Idempotency receipts are inserted in the same transaction as their command. */
public interface IdempotencyRepository {
    /** Serialize concurrent uses of one actor/scope/key within the current transaction. */
    void lock(String actorId, String scope, String key);
    Optional<Receipt> find(String actorId, String scope, String key);
    void insert(Receipt receipt);
    record Receipt(UUID id, String actorId, String scope, String idempotencyKey, String fingerprint, String resultJson) { }
}
