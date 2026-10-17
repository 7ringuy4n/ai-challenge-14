package com.papaya.claims.persistence;
import java.time.Instant; import java.util.UUID;
public record AuditRow(UUID auditId,UUID claimId,long sequence,Instant timestamp,String fromState,String toState,String userId,String role,String notes,String configVersion,String traceId) {}
