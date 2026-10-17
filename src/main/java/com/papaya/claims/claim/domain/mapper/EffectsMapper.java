package com.papaya.claims.claim.domain.mapper;

import com.papaya.claims.claim.domain.persistence.OutboxRow;
import com.papaya.claims.claim.domain.persistence.PaymentRow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EffectsMapper {
    int enqueue(OutboxRow row);
    List<OutboxRow> claimDue(@Param("now") Instant now, @Param("limit") int limit,
                             @Param("leaseMillis") long leaseMillis);
    int delivered(@Param("id") UUID id, @Param("at") Instant at);
    int failed(@Param("id") UUID id, @Param("error") String error, @Param("retry") Instant retry);
    int createPayment(PaymentRow row);
    int confirmPayment(@Param("claimId") UUID claimId, @Param("reference") String reference);
    boolean paymentExists(@Param("claimId") UUID claimId);
    PaymentRow paymentConfirmation(@Param("claimId") UUID claimId);
}
