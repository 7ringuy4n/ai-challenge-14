package com.papaya.claims.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Repository public final class MyBatisEffectRepository implements EffectRepository { private final EffectMapper mapper; private final ObjectMapper json; public MyBatisEffectRepository(EffectMapper mapper,ObjectMapper json){this.mapper=mapper;this.json=json;} public void enqueue(List<OutboxMessage> messages){for(var m:messages)mapper.enqueue(new OutboxRow(m.id(),m.claimId(),m.effect(),m.payload(),m.attempts(),"pending",Instant.EPOCH));} public List<OutboxMessage> pending(){return mapper.pending().stream().map(r->new OutboxMessage(r.id(),r.claimId(),r.effect(),r.payload(),r.attempts())).toList();} public void markDelivered(UUID id){mapper.markDelivered(id);} public void markFailed(UUID id,String error,Instant retryAt){mapper.markFailed(id,error,retryAt);} public void createPaymentRequest(UUID claimId,UUID requestId){mapper.createPaymentRequest(claimId,requestId);} public boolean paymentRequestExists(UUID claimId){return mapper.paymentRequestExists(claimId);} }
