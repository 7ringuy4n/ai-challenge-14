package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import com.papaya.claims.engine.*;
import com.papaya.claims.persistence.PostgresPersistenceSupport;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class TransitionAtomicityIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test void auditFailureRollsBackClaimUpdateAndOutbox() throws Exception {
        var h = PostgresPersistenceSupport.start(POSTGRES);
        UUID id = UUID.randomUUID();
        ClaimSnapshot claim = new ClaimSnapshot(id, "member-001", "SUBMITTED", new BigDecimal("25.00"), "USD",
                Map.of("requiredDocuments", List.of("receipt"), "documents", Map.of("receipt", true)), 0, 0, null, null);
        h.transactions().executeWithoutResult(s -> h.claims().insert(claim));
        AuditRepository failAudit = new AuditRepository() {
            @Override public void append(AuditEntry entry) { throw new IllegalStateException("injected audit failure"); }
            @Override public List<AuditEntry> findByClaimId(UUID claimId) { return h.audits().findByClaimId(claimId); }
        };
        ClaimService service = service(h, failAudit, h.effects());
        assertThatThrownBy(() -> service.transition(new Actor("clerk", "document_clerk"), id,
                new TransitionCommand("DOCUMENTS_VERIFIED", 0, "atomic-1", "verified")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("injected audit failure");
        assertThat(h.claims().find(id).orElseThrow().state()).isEqualTo("SUBMITTED");
        assertThat(h.audits().findByClaimId(id)).isEmpty();
        assertThat(h.effects().pending()).isEmpty();
        assertThat(h.commands().find(id, "clerk", "atomic-1")).isEmpty();
        outboxFailureRollsBack(h);
    }

    private void outboxFailureRollsBack(PostgresPersistenceSupport.Harness h) {
        UUID id = UUID.randomUUID();
        ClaimSnapshot claim = new ClaimSnapshot(id, "member-001", "SUBMITTED", new BigDecimal("25.00"), "USD",
                Map.of("requiredDocuments", List.of("receipt"), "documents", Map.of("receipt", true)), 0, 0, null, null);
        h.transactions().executeWithoutResult(s -> h.claims().insert(claim));
        EffectRepository failOutbox = new EffectRepository() {
            @Override public void enqueue(List<OutboxMessage> messages) { throw new IllegalStateException("injected outbox failure"); }
            @Override public List<OutboxMessage> pending() { return h.effects().pending(); }
            @Override public void markDelivered(UUID value) { h.effects().markDelivered(value); }
            @Override public void markFailed(UUID value, String error, java.time.Instant at) { h.effects().markFailed(value, error, at); }
            @Override public void createPaymentRequest(UUID claimId, UUID requestId) { h.effects().createPaymentRequest(claimId, requestId); }
            @Override public boolean paymentRequestExists(UUID claimId) { return h.effects().paymentRequestExists(claimId); }
        };
        assertThatThrownBy(() -> service(h, h.audits(), failOutbox).transition(new Actor("clerk", "document_clerk"), id,
                new TransitionCommand("DOCUMENTS_VERIFIED", 0, "outbox-1", "verified"))).isInstanceOf(IllegalStateException.class);
        assertThat(h.claims().find(id).orElseThrow().state()).isEqualTo("SUBMITTED");
        assertThat(h.audits().findByClaimId(id)).isEmpty(); assertThat(h.commands().find(id, "clerk", "outbox-1")).isEmpty();
    }
    static ClaimService service(PostgresPersistenceSupport.Harness h, AuditRepository audit, EffectRepository effects) {
        ObjectMapper json = h.json(); MockEffectFactory factory = new MockEffectFactory(json);
        return new ClaimService(h.claims(), audit, h.commands(), effects,
                new ConfiguredWorkflowEngine(WorkflowLoader.load(java.nio.file.Path.of("config/workflow.yml"))), h.transactions(),
                Clock.systemUTC(), factory, new EffectDispatcher(effects, factory, Clock.systemUTC()), json);
    }
}
