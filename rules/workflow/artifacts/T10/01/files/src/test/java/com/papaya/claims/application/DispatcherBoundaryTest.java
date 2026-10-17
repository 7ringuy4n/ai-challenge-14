package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.papaya.claims.contract.EffectRepository;
import com.papaya.claims.contract.Model.OutboxMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class DispatcherBoundaryTest {
    @Test void refusesPollingInsideAnAmbientTransactionWithoutTouchingTheRepository() {
        EffectRepository repository = mock(EffectRepository.class);
        try {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            assertThatThrownBy(() -> dispatcher(repository, mock(MockEffectFactory.class)).retryPending())
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("ambient transaction");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clear();
        }
        verifyNoInteractions(repository);
    }

    @Test void emptyPollAndNullPollResultDoNotDeliverAnything() {
        EffectRepository empty = mock(EffectRepository.class);
        MockEffectFactory factory = mock(MockEffectFactory.class);
        when(empty.pending()).thenReturn(List.of());
        dispatcher(empty, factory).retryPending();
        verifyNoInteractions(factory);

        EffectRepository nullResult = mock(EffectRepository.class);
        when(nullResult.pending()).thenReturn(null);
        dispatcher(nullResult, factory).retryPending();
        verifyNoInteractions(factory);
    }

    @Test void deliveryFailureSchedulesRetryWithBoundedExponentialDelayAndSurvivesRetryPersistenceFailure() {
        EffectRepository repository = mock(EffectRepository.class);
        MockEffectFactory factory = mock(MockEffectFactory.class);
        OutboxMessage message = new OutboxMessage(UUID.randomUUID(), UUID.randomUUID(), "notify_assessors", "{}", 2);
        when(repository.pending()).thenReturn(List.of(message));
        doThrow(new IllegalStateException("simulated delivery error")).when(factory).deliver(message);
        doThrow(new IllegalStateException("database unavailable")).when(repository).markFailed(eq(message.id()), anyString(), any());

        assertThatCode(() -> dispatcher(repository, factory).retryPending()).doesNotThrowAnyException();
        verify(repository).markFailed(eq(message.id()), eq("IllegalStateException"), eq(Instant.parse("2026-01-02T03:04:09Z")));
        verify(repository, never()).markDelivered(message.id());
    }

    @Test void successfulDeliveryIsMarkedDeliveredAfterDelivery() {
        EffectRepository repository = mock(EffectRepository.class);
        MockEffectFactory factory = mock(MockEffectFactory.class);
        OutboxMessage message = new OutboxMessage(UUID.randomUUID(), UUID.randomUUID(), "notify_assessors", "{}", 0);
        when(repository.pending()).thenReturn(List.of(message));
        dispatcher(repository, factory).retryPending();
        var order = inOrder(factory, repository);
        order.verify(factory).deliver(message);
        order.verify(repository).markDelivered(message.id());
        verify(repository, never()).markFailed(eq(message.id()), anyString(), any());
    }

    @Test void constructorConfiguresFreshTransactionPropagation() {
        EffectRepository repository = mock(EffectRepository.class);
        var manager = new RecordingTransactionManager();
        when(repository.pending()).thenReturn(List.of());
        new EffectDispatcher(repository, mock(MockEffectFactory.class), Clock.systemUTC(), manager).retryPending();
        assertThat(manager.propagations).contains(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static EffectDispatcher dispatcher(EffectRepository repository, MockEffectFactory factory) {
        return new EffectDispatcher(repository, factory,
                Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC), new RecordingTransactionManager());
    }

    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private final java.util.List<Integer> propagations = new java.util.ArrayList<>();
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { propagations.add(definition.getPropagationBehavior()); }
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }
}
