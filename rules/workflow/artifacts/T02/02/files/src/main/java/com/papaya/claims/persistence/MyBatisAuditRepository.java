package com.papaya.claims.persistence;

import com.papaya.claims.contract.AuditRepository;
import com.papaya.claims.contract.Model.AuditEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public final class MyBatisAuditRepository implements AuditRepository {
    private final AuditMapper mapper;

    public MyBatisAuditRepository(AuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void append(AuditEntry entry) {
        mapper.append(new AuditRow(
                entry.auditId(), entry.claimId(), entry.sequence(), entry.timestamp(),
                entry.fromState(), entry.toState(), entry.userId(), entry.role(), entry.notes(),
                entry.configVersion(), entry.traceId()));
    }

    @Override
    public List<AuditEntry> findByClaimId(UUID id) {
        return mapper.findByClaimId(id).stream().map(PersistenceJson::audit).toList();
    }
}
