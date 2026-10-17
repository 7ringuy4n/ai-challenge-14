package com.papaya.claims.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.ClaimRepository;
import com.papaya.claims.contract.Model.ClaimSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public final class MyBatisClaimRepository implements ClaimRepository {
    private final ClaimMapper mapper;
    private final ObjectMapper json;

    public MyBatisClaimRepository(ClaimMapper mapper, ObjectMapper json) {
        this.mapper = mapper;
        this.json = json;
    }

    @Override
    public void insert(ClaimSnapshot claim) {
        mapper.insert(PersistenceJson.row(json, claim));
    }

    @Override
    public Optional<ClaimSnapshot> find(UUID id) {
        return Optional.ofNullable(mapper.find(id)).map(row -> PersistenceJson.claim(json, row));
    }

    @Override
    @Transactional
    public Optional<ClaimSnapshot> findForUpdate(UUID id) {
        return Optional.ofNullable(mapper.findForUpdate(id))
                .map(row -> PersistenceJson.claim(json, row));
    }

    @Override
    public List<ClaimSnapshot> list() {
        return mapper.list().stream().map(row -> PersistenceJson.claim(json, row)).toList();
    }

    @Override
    public boolean compareAndSet(ClaimSnapshot updated, long expectedVersion) {
        return mapper.compareAndSet(PersistenceJson.row(json, updated), expectedVersion) == 1;
    }
}
