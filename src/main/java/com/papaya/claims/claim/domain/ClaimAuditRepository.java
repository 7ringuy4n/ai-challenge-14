package com.papaya.claims.claim.domain;

import java.util.UUID;

/** Append-only claim transition audit port. */
public interface ClaimAuditRepository {
    void append(AuditRecord record);
    PageResult<AuditRecord> findByClaim(UUID claimId, PageRequest page);
}
