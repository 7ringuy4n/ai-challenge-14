package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import com.papaya.claims.engine.ConfiguredWorkflowEngine;
import com.papaya.claims.engine.WorkflowLoader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

class TransitionServiceTest {
    @Test void approvalAtomicallyCreatesPaymentAndAuditAndDispatchesAfterExecuteReturns() {
        ClaimRepository claims = mock(ClaimRepository.class); AuditRepository audits = mock(AuditRepository.class);
        CommandRepository commands = mock(CommandRepository.class); EffectRepository effects = mock(EffectRepository.class);
        ClaimSnapshot claim = new ClaimSnapshot(UUID.randomUUID(), "member-001", "UNDER_ASSESSMENT", new BigDecimal("100.00"), "USD",
                Map.of("requiredDocuments", List.of("receipt"), "documents", Map.of("receipt", true), "reportComplete", true,
                        "assessmentAmount", new BigDecimal("80.00"), "assessmentCurrency", "USD"), 0, 2, Instant.EPOCH, null);
        when(claims.findForUpdate(claim.id())).thenReturn(Optional.of(claim)); when(commands.find(any(), any(), any())).thenReturn(Optional.empty());
        when(claims.compareAndSet(any(), eq(2L))).thenReturn(true);
        TransactionTemplate tx = new TransactionTemplate(new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
            @Override protected void doCommit(DefaultTransactionStatus status) {}
            @Override protected void doRollback(DefaultTransactionStatus status) {}
        });
        ObjectMapper json = JsonMapper.builder().findAndAddModules().build(); MockEffectFactory factory = new MockEffectFactory(json);
        MockEffectFactory factorySpy = spy(factory);
        ClaimService service = new ClaimService(claims, audits, commands, effects,
                new ConfiguredWorkflowEngine(WorkflowLoader.load(Path.of("config/workflow.yml"))), tx,
                Clock.fixed(Instant.parse("2026-01-02T03:04:05.123456789Z"), ZoneOffset.UTC), factorySpy, json);
        ClaimSnapshot result = service.transition(new Actor("assessor", "assessor"), claim.id(), new TransitionCommand("APPROVED", 2, "approve-1", "Approved"));
        assertThat(result.state()).isEqualTo("APPROVED"); assertThat(result.facts()).containsEntry("paymentRequestCreated", true);
        verify(effects).createPaymentRequest(eq(claim.id()), any()); verify(audits).append(argThat(a -> a.sequence() == 3 && a.configVersion().equals("1")));
        verify(effects).enqueue(argThat(list -> list.size() == 1 && list.getFirst().effect().equals("notify_approval")));
        verify(factorySpy, never()).deliver(any()); verify(commands).insert(eq(claim.id()), eq("assessor"), eq("approve-1"), any());
    }

    @Test void replayReturnsStoredResultBeforeCheckingStaleVersionAndRejectsChangedPayload() {
        ClaimRepository claims = mock(ClaimRepository.class); CommandRepository commands = mock(CommandRepository.class);
        ClaimSnapshot current = new ClaimSnapshot(UUID.randomUUID(), "member-001", "APPROVED", BigDecimal.TEN, "USD", Map.of(), 0, 4, null, null);
        ClaimSnapshot saved = new ClaimSnapshot(current.id(), current.memberId(), "APPROVED", BigDecimal.TEN, "USD", Map.of(), 0, 3, null, null);
        when(claims.findForUpdate(current.id())).thenReturn(Optional.of(current));
        ClaimService service = service(claims, commands, mock(AuditRepository.class), mock(EffectRepository.class));
        when(commands.find(current.id(), "finance", "key")).thenReturn(Optional.of(new CommandReceipt(hash("CLOSED", 2, "note", "finance"), saved)));
        assertThat(service.transition(new Actor("finance", "finance"), current.id(), new TransitionCommand("CLOSED", 2, "key", "note"))).isEqualTo(saved);
        assertThatThrownBy(() -> service.transition(new Actor("finance", "finance"), current.id(), new TransitionCommand("CLOSED", 2, "key", "other")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("different command");
        verify(claims, never()).compareAndSet(any(), anyLong());
    }
    private static String hash(String target, long version, String notes, String role) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((target+"\u0000"+version+"\u0000"+notes+"\u0000"+role).getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    static ClaimService service(ClaimRepository claims, CommandRepository commands, AuditRepository audits, EffectRepository effects) {
        var json = JsonMapper.builder().findAndAddModules().build(); var factory = new MockEffectFactory(json);
        return new ClaimService(claims, audits, commands, effects, new ConfiguredWorkflowEngine(WorkflowLoader.load(Path.of("config/workflow.yml"))),
                new TransactionTemplate(new AbstractPlatformTransactionManager() {
                    @Override protected Object doGetTransaction(){return new Object();}
                    @Override protected void doBegin(Object t, TransactionDefinition d){}
                    @Override protected void doCommit(DefaultTransactionStatus s){}
                    @Override protected void doRollback(DefaultTransactionStatus s){}
                }), Clock.systemUTC(), factory, json);
    }
}
