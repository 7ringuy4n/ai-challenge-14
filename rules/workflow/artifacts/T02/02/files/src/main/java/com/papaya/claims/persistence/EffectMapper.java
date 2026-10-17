package com.papaya.claims.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EffectMapper {
    int enqueue(OutboxRow row);
    List<OutboxRow> pending();
    int markDelivered(UUID id);
    int markFailed(@Param("id") UUID id, @Param("error") String error,
                   @Param("retryAt") Instant retryAt);
    int createPaymentRequest(@Param("claimId") UUID claimId, @Param("requestId") UUID requestId);
    boolean paymentRequestExists(UUID claimId);
}
