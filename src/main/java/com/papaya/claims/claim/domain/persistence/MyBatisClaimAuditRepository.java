package com.papaya.claims.claim.domain.persistence;

import com.papaya.claims.claim.domain.*;
import com.papaya.claims.claim.domain.mapper.AuditMapper;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisClaimAuditRepository implements ClaimAuditRepository {
    private final AuditMapper mapper;
    public MyBatisClaimAuditRepository(AuditMapper mapper) { this.mapper=mapper; }
    @Override public void append(AuditRecord a) {
        AuditRow r=new AuditRow(); r.id=a.id(); r.claimId=a.claimId(); r.sequence=a.sequence(); r.occurredAt=a.occurredAt();
        r.fromState=a.fromState(); r.toState=a.toState(); r.actorId=a.actorId(); r.role=a.role(); r.reason=a.reason();
        r.correlationId=a.correlationId(); r.workflowVersion=a.workflowVersion(); mapper.append(r);
    }
    @Override public PageResult<AuditRecord> findByClaim(UUID id, PageRequest p) {
        var items=mapper.page(id,p.size(),p.offset()).stream().map(r -> new AuditRecord(r.id,r.claimId,r.sequence,
                r.occurredAt,r.fromState,r.toState,r.actorId,r.role,r.reason,r.correlationId,r.workflowVersion)).toList();
        return new PageResult<>(items,mapper.count(id),p);
    }
}
