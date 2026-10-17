package com.papaya.claims.claim.domain.persistence;

import com.papaya.claims.claim.domain.SystemOperationAuditRecord;
import com.papaya.claims.claim.domain.SystemOperationAuditRepository;
import com.papaya.claims.claim.domain.mapper.SystemAuditMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSystemOperationAuditRepository implements SystemOperationAuditRepository {
    private final SystemAuditMapper mapper;
    public MyBatisSystemOperationAuditRepository(SystemAuditMapper mapper) { this.mapper=mapper; }
    @Override public void append(SystemOperationAuditRecord a) {
        SystemAuditRow r=new SystemAuditRow(); r.id=a.id(); r.action=a.action(); r.resource=a.resource(); r.resourceId=a.resourceId();
        r.actorId=a.actorId(); r.correlationId=a.correlationId(); r.outcome=a.outcome(); r.errorType=a.errorType();
        r.occurredAt=a.occurredAt(); mapper.append(r);
    }
}
