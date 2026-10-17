package com.papaya.claims.audit;

import com.papaya.claims.claim.domain.Actor;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ClaimFailureAuditAspect {
    private static final Logger log = LoggerFactory.getLogger(ClaimFailureAuditAspect.class);
    private final FailureAuditWriter writer;
    public ClaimFailureAuditAspect(FailureAuditWriter writer) { this.writer = writer; }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "failure")
    public void recordFailure(JoinPoint call, Throwable failure, Auditable auditable) {
        Actor actor = null; UUID claimId = null; String correlationId = null;
        Object[] args = call.getArgs();
        for (Object arg : args) {
            if (arg instanceof Actor value) actor = value;
            else if (arg instanceof UUID value) claimId = value;
            else if (arg instanceof String value) correlationId = value;
        }
        try {
            writer.record(auditable.action(), auditable.resource(), claimId == null ? null : claimId.toString(),
                    actor, correlationId, failure);
        } catch (RuntimeException auditFailure) {
            log.error("failure_audit_write_failed operation={} errorType={}", call.getSignature().getName(), auditFailure.getClass().getSimpleName());
        }
    }
}
