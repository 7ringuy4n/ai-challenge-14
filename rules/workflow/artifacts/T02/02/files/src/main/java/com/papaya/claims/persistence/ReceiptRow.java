package com.papaya.claims.persistence;

import java.util.UUID;

public record ReceiptRow(
        UUID claimId, String userId, String idempotencyKey, String requestHash, String result) {}
