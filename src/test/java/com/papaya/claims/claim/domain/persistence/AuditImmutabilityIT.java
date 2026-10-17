package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.papaya.claims.claim.domain.AuditRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditImmutabilityIT extends PersistenceITSupport {
    @Test
    void auditRepositoryAppendsAndReadsAndRuntimeCannotRewriteEitherAuditTable() throws Exception {
        UUID claimId=rawClaim(); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        AuditRecord record=new AuditRecord(UUID.randomUUID(),claimId,0,now,null,"submitted","member-1","member","created","corr-1","v1");
        try(var s=session()) {
            var repo=auditRepository(s); repo.append(record);
            assertThat(repo.findByClaim(claimId,new com.papaya.claims.claim.domain.PageRequest(0,10)).items()).containsExactly(record);
        }
        try(var c=runtime(); var p=c.prepareStatement("INSERT INTO system_operation_audit(id,action,resource,resource_id,actor_id,correlation_id,outcome,occurred_at) VALUES(?, 'claim.read','claim',?,'member-1','corr-1','SUCCESS',clock_timestamp())")) {
            p.setObject(1,UUID.randomUUID()); p.setString(2,claimId.toString()); p.executeUpdate();
        }
        try(var c=runtime(); var s=c.createStatement()) {
            for(String table: List.of("claim_audit","system_operation_audit")) {
                String where=table.equals("claim_audit") ? "claim_id='"+claimId+"'" : "resource_id='"+claimId+"'";
                assertThat(catchThrowable(()->s.executeUpdate("UPDATE "+table+" SET "+(table.equals("claim_audit")?"reason='rewritten'":"outcome='FAILED'")+" WHERE "+where))).as(table+" UPDATE").isNotNull();
                assertThat(catchThrowable(()->s.executeUpdate("DELETE FROM "+table+" WHERE "+where))).as(table+" DELETE").isNotNull();
            }
            try(var r=s.executeQuery("SELECT (SELECT count(*) FROM claim_audit WHERE claim_id='"+claimId+"'),(SELECT count(*) FROM system_operation_audit WHERE resource_id='"+claimId+"')")) {
                assertThat(r.next()).isTrue(); assertThat(r.getLong(1)).isEqualTo(1); assertThat(r.getLong(2)).isEqualTo(1);
            }
        }
    }
}
