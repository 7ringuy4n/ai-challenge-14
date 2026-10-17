package com.papaya.claims.claim.domain;

import java.util.Optional;
import java.util.UUID;

/** Persistence port. Mutating methods participate in the application transaction. */
public interface ClaimRepository {
    void insert(ClaimAggregate claim);
    Optional<ClaimAggregate> find(UUID claimId);
    Optional<ClaimAggregate> findForUpdate(UUID claimId);
    PageResult<ClaimAggregate> listVisible(PageRequest page, Actor actor);
    boolean compareAndSet(ClaimAggregate claim, long expectedVersion);
}
