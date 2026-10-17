package com.papaya.claims.claim.domain.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.IdempotencyRepository;
import com.papaya.claims.claim.domain.mapper.ReceiptMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisIdempotencyRepository implements IdempotencyRepository {
    private final ReceiptMapper mapper; private final ObjectMapper json;
    public MyBatisIdempotencyRepository(ReceiptMapper mapper,ObjectMapper json) { this.mapper=mapper; this.json=json; }
    @Override public void lock(String actor, String scope, String key) { mapper.lock(actor, scope, key); }
    @Override public Optional<Receipt> find(String actor,String scope,String key) {
        ReceiptRow r=mapper.find(actor,scope,key);
        if(r==null) return Optional.empty();
        return Optional.of(new Receipt(r.id,r.actorId,r.scope,r.idempotencyKey,r.fingerprint,r.resultJson));
    }
    @Override public void insert(Receipt receipt) {
        ReceiptRow r=new ReceiptRow(); r.id=receipt.id(); r.actorId=receipt.actorId(); r.scope=receipt.scope();
        r.idempotencyKey=receipt.idempotencyKey(); r.fingerprint=receipt.fingerprint(); r.resultJson=receipt.resultJson();
        mapper.insert(r); // Unique(actor,scope,key) violations propagate for service-level race/replay handling.
    }
}
