package com.papaya.claims.claim.domain.persistence;

import java.util.UUID;

public class ReceiptRow {
    public UUID id; public String actorId, scope, idempotencyKey, fingerprint, resultJson;
}
