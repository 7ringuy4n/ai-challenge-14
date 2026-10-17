package com.papaya.claims.application;

import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.papaya.claims.contract.EffectRepository;
import com.papaya.claims.contract.Model.OutboxMessage;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EffectDispatcherTest {
    @Test void failedConsoleDeliveryIsMarkedRetryableNotDelivered() {
        EffectRepository repository = mock(EffectRepository.class);
        MockEffectFactory factory = mock(MockEffectFactory.class);
        OutboxMessage message = new OutboxMessage(UUID.randomUUID(), UUID.randomUUID(), "notify_assessors", "{}", 0);
        doThrow(new IllegalStateException("simulated console failure")).when(factory).deliver(message);
        new EffectDispatcher(repository, factory, Clock.systemUTC()).dispatch(java.util.List.of(message));
        verify(repository, never()).markDelivered(message.id());
        verify(repository).markFailed(eq(message.id()), eq("IllegalStateException"), any());
    }
    @Test void successfulDeliveryMarksOutboxDelivered() {
        EffectRepository repository = mock(EffectRepository.class);
        MockEffectFactory factory = new MockEffectFactory(JsonMapper.builder().findAndAddModules().build());
        OutboxMessage message = new OutboxMessage(UUID.randomUUID(), UUID.randomUUID(), "notify_assessors", "{}", 0);
        new EffectDispatcher(repository, factory, Clock.systemUTC()).dispatch(java.util.List.of(message));
        verify(repository).markDelivered(message.id());
        verify(repository, never()).markFailed(eq(message.id()), anyString(), any());
    }
}
