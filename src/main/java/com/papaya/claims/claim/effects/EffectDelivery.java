package com.papaya.claims.claim.effects;

import com.papaya.claims.claim.domain.OutboxRepository;

/** Delivery boundary for post-commit effects. Implementations must tolerate at-least-once delivery. */
public interface EffectDelivery {
    void deliver(OutboxRepository.Message message);
}
