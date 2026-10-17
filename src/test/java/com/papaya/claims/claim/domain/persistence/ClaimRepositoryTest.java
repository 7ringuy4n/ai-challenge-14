package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimAggregate;
import com.papaya.claims.claim.domain.ClaimFacts;
import com.papaya.claims.claim.domain.ClaimWorkflowData;
import com.papaya.claims.claim.domain.PageRequest;
import com.papaya.claims.claim.domain.mapper.ClaimMapper;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClaimRepositoryTest {
    @Test
    void scopesMembersButAllowsAdminAndSystemViewsAndMapsEmptyAndPresentRows() {
        ClaimRow row = row();
        Object[] returned = {row};
        Object[] lastPageMethod = {null};
        ClaimMapper mapper = mapper((method, args) -> {
            if (method.equals("find") || method.equals("findForUpdate")) return returned[0];
            if (method.equals("pageMember") || method.equals("pageAll")) { lastPageMethod[0] = method; return List.of(row); }
            if (method.equals("countMember")) return 1L;
            if (method.equals("countAll")) return 7L;
            return 0;
        });
        var repository = new MyBatisClaimRepository(mapper, new ObjectMapper());
        assertThat(repository.find(row.id)).isPresent();
        assertThat(repository.findForUpdate(row.id)).isPresent();
        assertThat(repository.listVisible(new PageRequest(0, 10), new Actor("member-1", java.util.Set.of("member"), false)).total()).isEqualTo(1);
        assertThat(lastPageMethod[0]).isEqualTo("pageMember");
        assertThat(repository.listVisible(new PageRequest(0, 10), new Actor("member-1", java.util.Set.of("member", "claims_admin"), false)).total()).isEqualTo(7);
        assertThat(lastPageMethod[0]).isEqualTo("pageAll");
        assertThat(repository.listVisible(new PageRequest(0, 10), new Actor("system", java.util.Set.of("member", "system"), true)).total()).isEqualTo(7);

        returned[0] = null;
        assertThat(repository.find(row.id)).isEmpty();
        assertThat(repository.findForUpdate(row.id)).isEmpty();
    }

    @Test
    void mapsWritesAndReturnsCompareAndSetOutcome() {
        Object[] result = {1};
        Object[] written = {null};
        ClaimMapper mapper = mapper((method, args) -> {
            if (method.equals("insert")) { written[0] = args[0]; return 1; }
            if (method.equals("compareAndSet")) { written[0] = args[0]; return result[0]; }
            return null;
        });
        var repository = new MyBatisClaimRepository(mapper, new ObjectMapper());
        Claim claim = new Claim(UUID.randomUUID(), "member-1", "SUBMITTED", new BigDecimal("9.00"), "USD", 0,
                new ClaimFacts(Map.of()), Instant.EPOCH, Instant.EPOCH);
        ClaimAggregate aggregate = new ClaimAggregate(claim, ClaimWorkflowData.empty());
        repository.insert(aggregate);
        assertThat(written[0]).isInstanceOf(ClaimRow.class);
        assertThat(repository.compareAndSet(aggregate, 0)).isTrue();
        result[0] = 0;
        assertThat(repository.compareAndSet(aggregate, 0)).isFalse();
    }

    private static ClaimRow row() {
        ClaimRow row = new ClaimRow(); row.id = UUID.randomUUID(); row.memberId = "member-1";
        row.state = "SUBMITTED"; row.policyLimit = new BigDecimal("100.00"); row.currency = "USD";
        row.version = 1; row.factsJson = "{}"; row.createdAt = Instant.EPOCH; row.updatedAt = Instant.EPOCH;
        return row;
    }
    @FunctionalInterface private interface Invocation { Object call(String method, Object[] args) throws Exception; }
    private static ClaimMapper mapper(Invocation invocation) {
        return (ClaimMapper) Proxy.newProxyInstance(ClaimMapper.class.getClassLoader(), new Class<?>[]{ClaimMapper.class},
                (proxy, method, args) -> invocation.call(method.getName(), args == null ? new Object[0] : args));
    }
}
