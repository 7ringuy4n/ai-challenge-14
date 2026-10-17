package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import com.papaya.claims.engine.*;
import com.papaya.claims.persistence.PostgresPersistenceSupport;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
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
        outerTransactionRollbackDoesNotDeliver(h);
    }

    private void outerTransactionRollbackDoesNotDeliver(PostgresPersistenceSupport.Harness h) {
        UUID id = UUID.randomUUID();
        ClaimSnapshot claim = new ClaimSnapshot(id, "member-001", "SUBMITTED", new BigDecimal("25.00"), "USD",
                Map.of("requiredDocuments", List.of("receipt"), "documents", Map.of("receipt", true)), 0, 0, null, null);
        h.transactions().executeWithoutResult(s -> h.claims().insert(claim));
        MockEffectFactory factory = spy(new MockEffectFactory(h.json()));
        ClaimService service = service(h, h.audits(), h.effects(), factory);
        AtomicBoolean uncommittedOutboxWasVisible = new AtomicBoolean(false);
        assertThatThrownBy(() -> h.transactions().executeWithoutResult(status -> {
            service.transition(new Actor("clerk", "document_clerk"), id,
                    new TransitionCommand("DOCUMENTS_VERIFIED", 0, "outer-rollback", "verified"));
            uncommittedOutboxWasVisible.set(!h.effects().pending().isEmpty());
            throw new IllegalStateException("rollback enclosing transaction");
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("rollback enclosing transaction");
        assertThat(uncommittedOutboxWasVisible).hasValue(true);
        assertThat(h.claims().find(id).orElseThrow().state()).isEqualTo("SUBMITTED");
        assertThat(h.audits().findByClaimId(id)).isEmpty();
        assertThat(h.effects().pending()).isEmpty();
        assertThat(h.commands().find(id, "clerk", "outer-rollback")).isEmpty();
        var dispatcher = new EffectDispatcher(h.effects(), factory, Clock.systemUTC(), new DataSourceTransactionManager(h.dataSource()));
        dispatcher.retryPending();
        verify(factory, never()).deliver(any());
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
        return service(h, audit, effects, new MockEffectFactory(h.json()));
    }
    static ClaimService service(PostgresPersistenceSupport.Harness h, AuditRepository audit, EffectRepository effects, MockEffectFactory factory) {
        ObjectMapper json = h.json();
        return new ClaimService(h.claims(), audit, h.commands(), effects,
                new ConfiguredWorkflowEngine(WorkflowLoader.load(java.nio.file.Path.of("config/workflow.yml"))), h.transactions(),
                Clock.systemUTC(), factory, json);
    }
}
