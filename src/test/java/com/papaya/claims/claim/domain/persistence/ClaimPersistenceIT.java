package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.papaya.claims.claim.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClaimPersistenceIT extends PersistenceITSupport {
    @Test
    void migrationOwnsSchemaAndRuntimeCannotCreateObjects() throws Exception {
        try (var c=runtime(); var s=c.createStatement(); var r=s.executeQuery("SELECT rolname,rolsuper,rolcreatedb,rolcreaterole FROM pg_roles WHERE rolname IN ('claims_migration','claims_runtime') ORDER BY rolname")) {
            assertThat(r.next()).isTrue(); assertThat(r.getString(1)).isEqualTo("claims_migration");
            assertThat(r.getBoolean(2)).isFalse(); assertThat(r.getBoolean(3)).isFalse(); assertThat(r.getBoolean(4)).isFalse();
            assertThat(r.next()).isTrue(); assertThat(r.getString(1)).isEqualTo("claims_runtime");
            assertThat(r.getBoolean(2)).isFalse(); assertThat(r.getBoolean(3)).isFalse(); assertThat(r.getBoolean(4)).isFalse(); assertThat(r.next()).isFalse();
        }
        try (var c=runtime(); var s=c.createStatement(); var r=s.executeQuery("SELECT has_schema_privilege('claims_migration','public','CREATE'),has_schema_privilege('claims_runtime','public','USAGE'),has_schema_privilege('claims_runtime','public','CREATE')")) {
            assertThat(r.next()).isTrue(); assertThat(r.getBoolean(1)).isTrue(); assertThat(r.getBoolean(2)).isTrue(); assertThat(r.getBoolean(3)).isFalse();
        }
        try (var c=runtime(); var s=c.createStatement(); var r=s.executeQuery("SELECT has_column_privilege('claims_runtime','claims','member_id','UPDATE'),has_column_privilege('claims_runtime','claims','policy_limit','UPDATE'),has_column_privilege('claims_runtime','claims','currency','UPDATE'),has_column_privilege('claims_runtime','claims','state','UPDATE')")) {
            assertThat(r.next()).isTrue(); assertThat(r.getBoolean(1)).isFalse(); assertThat(r.getBoolean(2)).isFalse(); assertThat(r.getBoolean(3)).isFalse(); assertThat(r.getBoolean(4)).isTrue();
        }
        try (var c=runtime(); var s=c.createStatement()) {
            assertThat(catchThrowable(() -> s.execute("CREATE TABLE runtime_must_not_create(id integer)"))).isNotNull();
        }
    }

    @Test
    void repositoryRoundTripListsAndCasLeaveImmutableIdentityAndCoverageUntouched() {
        UUID id=UUID.randomUUID(); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        BigDecimal nested=new BigDecimal("42.375");
        BigDecimal wholeDollar=new BigDecimal("125.00");
        ClaimAggregate original=new ClaimAggregate(new Claim(id,"member-1","submitted",new BigDecimal("125.00"),"USD",0,
                new ClaimFacts(Map.of("requestedAmount",wholeDollar,"nested",Map.of("amount",nested))),now,now),ClaimWorkflowData.empty());
        try (var s=session()) {
            var repo=claimRepository(s); repo.insert(original);
            ClaimAggregate found=repo.find(id).orElseThrow();
            assertThat(found.claim().facts().values().get("nested")).isInstanceOf(Map.class);
            Object actual=((Map<?,?>)found.claim().facts().values().get("nested")).get("amount");
            assertThat(actual).isInstanceOf(BigDecimal.class).isEqualTo(nested);
            assertThat(found.claim().facts().values().get("requestedAmount")).isInstanceOf(BigDecimal.class).isEqualTo(wholeDollar);
            assertThat(found.claim().memberId()).isEqualTo("member-1");
            assertThat(found.claim().policyLimit()).isEqualByComparingTo("125.00");
            assertThat(found.claim().currency()).isEqualTo("USD");
            assertThat(repo.listVisible(new PageRequest(0,10),new Actor("member-1",Set.of("member"),false)).items()).extracting(a->a.claim().id()).contains(id);
            ClaimAggregate attempted=new ClaimAggregate(new Claim(id,"attacker","assessing",new BigDecimal("1.00"),"EUR",0,
                    new ClaimFacts(Map.of("changed",true)),now,now),ClaimWorkflowData.empty());
            assertThat(repo.compareAndSet(attempted,0)).isTrue();
            Claim unchanged=repo.find(id).orElseThrow().claim();
            assertThat(unchanged.state()).isEqualTo("assessing");
            assertThat(unchanged.memberId()).isEqualTo("member-1");
            assertThat(unchanged.policyLimit()).isEqualByComparingTo("125.00");
            assertThat(unchanged.currency()).isEqualTo("USD");
            assertThat(repo.compareAndSet(attempted,0)).isFalse();
        }
    }
}
