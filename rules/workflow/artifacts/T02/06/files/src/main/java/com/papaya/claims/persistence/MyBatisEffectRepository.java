package com.papaya.claims.persistence;

import com.papaya.claims.contract.EffectRepository;
import com.papaya.claims.contract.Model.OutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisEffectRepository implements EffectRepository {
    private final EffectMapper mapper;

    public MyBatisEffectRepository(EffectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void enqueue(List<OutboxMessage> messages) {
        for (OutboxMessage message : messages) {
            mapper.enqueue(new OutboxRow(message.id(), message.claimId(), message.effect(),
                    message.payload(), message.attempts(), "pending", Instant.EPOCH));
        }
    }

    @Override
    public List<OutboxMessage> pending() {
        return mapper.pending().stream()
                .map(row -> new OutboxMessage(
                        row.id(), row.claimId(), row.effect(), row.payload(), row.attempts()))
                .toList();
    }

    @Override
    public void markDelivered(UUID id) {
        mapper.markDelivered(id);
    }

    @Override
    public void markFailed(UUID id, String error, Instant retryAt) {
        mapper.markFailed(id, error, retryAt);
    }

    @Override
    public void createPaymentRequest(UUID claimId, UUID requestId) {
        mapper.createPaymentRequest(claimId, requestId);
    }

    @Override
    public boolean paymentRequestExists(UUID claimId) {
        return mapper.paymentRequestExists(claimId);
    }
}
