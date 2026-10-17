package com.papaya.claims.application;

import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.papaya.claims.contract.EffectRepository;
import com.papaya.claims.contract.Model.OutboxMessage;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class EffectDispatcherTest {
    @Test void failedDeliveryIsMarkedRetryableNotDelivered() {
        EffectRepository repository = mock(EffectRepository.class);
        MockEffectFactory factory = mock(MockEffectFactory.class);
        OutboxMessage message = message(); when(repository.pending()).thenReturn(List.of(message));
        doThrow(new IllegalStateException("simulated console failure")).when(factory).deliver(message);
        dispatcher(repository, factory).retryPending();
        verify(repository, never()).markDelivered(message.id());
        verify(repository).markFailed(eq(message.id()), eq("IllegalStateException"), any());
    }
    @Test void committedPendingEffectIsDeliveredAndMarked() {
        EffectRepository repository = mock(EffectRepository.class);
        MockEffectFactory factory = new MockEffectFactory(JsonMapper.builder().findAndAddModules().build());
        OutboxMessage message = message(); when(repository.pending()).thenReturn(List.of(message));
        dispatcher(repository, factory).retryPending();
        verify(factory).deliver(message); verify(repository).markDelivered(message.id());
        verify(repository, never()).markFailed(eq(message.id()), anyString(), any());
    }
    private static OutboxMessage message() { return new OutboxMessage(UUID.randomUUID(), UUID.randomUUID(), "notify_assessors", "{}", 0); }
    private static EffectDispatcher dispatcher(EffectRepository repository, MockEffectFactory factory) {
        PlatformTransactionManager manager = new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
            @Override protected void doCommit(DefaultTransactionStatus status) {}
            @Override protected void doRollback(DefaultTransactionStatus status) {}
        };
        return new EffectDispatcher(repository, factory, Clock.systemUTC(), manager);
    }
}
