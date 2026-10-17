package com.papaya.claims.persistence;
import org.apache.ibatis.annotations.*; import java.util.*;
@Mapper public interface EffectMapper { int enqueue(OutboxRow row); List<OutboxRow> pending(); int markDelivered(UUID id); int markFailed(@Param("id") UUID id,@Param("error") String error,@Param("retryAt") java.time.Instant retryAt); int createPaymentRequest(@Param("claimId") UUID claimId,@Param("requestId") UUID requestId); boolean paymentRequestExists(UUID claimId); }
