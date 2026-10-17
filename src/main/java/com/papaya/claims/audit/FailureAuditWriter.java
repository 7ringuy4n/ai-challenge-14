package com.papaya.claims.audit;

import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.SystemOperationAuditRecord;
import com.papaya.claims.claim.domain.SystemOperationAuditRepository;
import com.papaya.claims.claim.service.BusinessException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records failed mutation attempts in a separate transaction, even when the business transaction rolls back. */
@Service
public class FailureAuditWriter {
    private static final Pattern SAFE_CORRELATION = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private final SystemOperationAuditRepository audit;
    private final Clock clock;
    public FailureAuditWriter(SystemOperationAuditRepository audit, Clock clock) { this.audit = audit; this.clock = clock; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resource, String resourceId, Actor actor,
            String correlationId, Throwable failure) {
        if (actor == null) return;
        String errorType = failure instanceof BusinessException business ? business.code() : failure.getClass().getSimpleName();
        String safeCorrelation = correlationId != null && SAFE_CORRELATION.matcher(correlationId).matches()
                ? correlationId : UUID.randomUUID().toString();
        audit.append(new SystemOperationAuditRecord(UUID.randomUUID(), action, resource, resourceId,
                actor.id(), safeCorrelation,
                "FAILED", errorType, clock.instant().truncatedTo(ChronoUnit.MICROS)));
    }
}
