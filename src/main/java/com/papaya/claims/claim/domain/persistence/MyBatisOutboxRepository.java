package com.papaya.claims.claim.domain.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.OutboxRepository;
import com.papaya.claims.claim.domain.mapper.EffectsMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;

@Repository
public class MyBatisOutboxRepository implements OutboxRepository {
    private final EffectsMapper mapper; private final long leaseMillis;
    public MyBatisOutboxRepository(EffectsMapper mapper,
            @Value("${claims.outbox.lease-duration-ms:30000}") long leaseMillis) {
        if (leaseMillis < 1) throw new IllegalArgumentException("Outbox lease duration must be positive");
        this.mapper=mapper; this.leaseMillis=leaseMillis;
    }
    @Override public void enqueue(Message m) {
        OutboxRow r=new OutboxRow(); r.id=m.id(); r.claimId=m.claimId(); r.effect=m.effect(); r.payloadJson=m.payload();
        r.attempts=m.attempts(); r.availableAt=m.availableAt(); r.traceparent=m.traceparent(); mapper.enqueue(r);
    }
    @Override public List<Message> claimDue(Instant now,int limit) {
        if(limit<1) throw new IllegalArgumentException("limit must be positive");
        return mapper.claimDue(now,limit,leaseMillis).stream().map(r -> new Message(r.id,r.claimId,r.effect,r.payloadJson,r.attempts,r.availableAt,r.traceparent)).toList();
    }
    @Override public boolean markDelivered(UUID id,Instant at) { return mapper.delivered(id,at)==1; }
    @Override public boolean markFailed(UUID id,String safeError,Instant retryAt) { return mapper.failed(id,safeError,retryAt)==1; }
}
