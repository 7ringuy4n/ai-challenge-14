package com.papaya.claims.workflow;

import java.util.Map;
import java.util.Objects;

/** Pure effect plan. The application service persists it to the outbox in the claim transaction. */
public record EffectIntent(String id, Map<String, Object> payload) {
    public EffectIntent {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Effect id is required");
        payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
    }
}
