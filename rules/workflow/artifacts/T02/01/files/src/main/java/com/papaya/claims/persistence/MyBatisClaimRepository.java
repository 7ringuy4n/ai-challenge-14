package com.papaya.claims.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Repository public final class MyBatisClaimRepository implements ClaimRepository { private final ClaimMapper mapper; private final ObjectMapper json; public MyBatisClaimRepository(ClaimMapper mapper,ObjectMapper json){this.mapper=mapper;this.json=json;} public void insert(ClaimSnapshot c){mapper.insert(PersistenceJson.row(json,c));} public Optional<ClaimSnapshot> find(UUID id){return Optional.ofNullable(mapper.find(id)).map(r->PersistenceJson.claim(json,r));} @Transactional public Optional<ClaimSnapshot> findForUpdate(UUID id){return Optional.ofNullable(mapper.findForUpdate(id)).map(r->PersistenceJson.claim(json,r));} public List<ClaimSnapshot> list(){return mapper.list().stream().map(r->PersistenceJson.claim(json,r)).toList();} public boolean compareAndSet(ClaimSnapshot c,long expected){return mapper.compareAndSet(PersistenceJson.row(json,c),expected)==1;} }
