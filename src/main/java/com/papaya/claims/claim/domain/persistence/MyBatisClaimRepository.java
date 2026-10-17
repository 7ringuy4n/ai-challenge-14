package com.papaya.claims.claim.domain.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.*;
import com.papaya.claims.claim.domain.mapper.ClaimMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisClaimRepository implements ClaimRepository {
    private final ClaimMapper mapper; private final ObjectMapper json;
    public MyBatisClaimRepository(ClaimMapper mapper, ObjectMapper json) { this.mapper=mapper; this.json=json; }
    @Override public void insert(ClaimAggregate claim) { mapper.insert(PersistenceMapping.row(json, claim)); }
    @Override public Optional<ClaimAggregate> find(java.util.UUID id) { return Optional.ofNullable(mapper.find(id)).map(r -> PersistenceMapping.aggregate(json,r)); }
    @Override public Optional<ClaimAggregate> findForUpdate(java.util.UUID id) { return Optional.ofNullable(mapper.findForUpdate(id)).map(r -> PersistenceMapping.aggregate(json,r)); }
    @Override public PageResult<ClaimAggregate> listVisible(PageRequest page, Actor actor) {
        boolean memberScoped = actor.hasRole("member") && !actor.system() && !actor.hasRole("claims_admin");
        var rows = memberScoped ? mapper.pageMember(actor.id(),page.size(),page.offset()) : mapper.pageAll(page.size(),page.offset());
        long total = memberScoped ? mapper.countMember(actor.id()) : mapper.countAll();
        return new PageResult<>(rows.stream().map(r -> PersistenceMapping.aggregate(json,r)).toList(), total, page);
    }
    @Override public boolean compareAndSet(ClaimAggregate claim, long expectedVersion) {
        return mapper.compareAndSet(PersistenceMapping.row(json, claim), expectedVersion) == 1;
    }
}
