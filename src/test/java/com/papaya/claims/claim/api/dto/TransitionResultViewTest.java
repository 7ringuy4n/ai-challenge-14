package com.papaya.claims.claim.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.papaya.claims.claim.domain.AuditRecord;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimFacts;
import com.papaya.claims.claim.domain.TransitionResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransitionResultViewTest {
    @Test
    void mapsClaimAuditAndReplayStatusToTheDocumentedTransportResult() {
        var claimId = UUID.randomUUID();
        var auditId = UUID.randomUUID();
        var timestamp = Instant.parse("2026-10-17T00:00:00Z");
        var claim = new Claim(claimId, "member-1", "APPROVED", new BigDecimal("150.00"), "USD", 2,
                new ClaimFacts(Map.of("documents", java.util.List.of("invoice"))), timestamp, timestamp);
        var audit = new AuditRecord(auditId, claimId, 2, timestamp, "ASSESSMENT", "APPROVED", "agent-1",
                "assessor", "verified", "correlation-1", "1");

        var view = TransitionResultView.from(new TransitionResult(claim, audit, true));

        assertThat(view.claim().id()).isEqualTo(claimId);
        assertThat(view.claim().state()).isEqualTo("APPROVED");
        assertThat(view.audit().id()).isEqualTo(auditId);
        assertThat(view.audit().sequence()).isEqualTo(2);
        assertThat(view.replayed()).isTrue();
    }
}
