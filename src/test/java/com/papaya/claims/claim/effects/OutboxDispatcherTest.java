package com.papaya.claims.claim.effects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.claim.domain.OutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OutboxDispatcherTest {
    private static final Instant NOW = Instant.parse("2026-10-17T02:00:00Z");

    @Test
    void claimsBoundedBatchDeliversAndMarksSuccessfulEvents() {
        var message = message("notify_member_approval");
        var outbox = new MemoryOutbox(List.of(message));
        var delivered = new ArrayList<OutboxRepository.Message>();
        var metrics = new SimpleMeterRegistry();
        var dispatcher = dispatcher(outbox, delivered::add, metrics, 5_000, 4);

        dispatcher.dispatchDue();

        assertThat(outbox.claimedAt).isEqualTo(NOW);
        assertThat(outbox.limit).isEqualTo(4);
        assertThat(delivered).containsExactly(message);
        assertThat(outbox.delivered).containsExactly(message.id());
        assertThat(metrics.get("claims.outbox.delivered").tag("effect", message.effect()).counter().count()).isEqualTo(1);
        assertThat(outbox.failed).isEmpty();
    }

    @Test
    void deliveryFailureSchedulesRetryAndRecordsFailureMetric() {
        var message = message("trigger_payment_system");
        var outbox = new MemoryOutbox(List.of(message));
        var metrics = new SimpleMeterRegistry();
        var dispatcher = dispatcher(outbox, ignored -> { throw new IllegalStateException("sensitive details"); }, metrics, 5_000, 4);

        dispatcher.dispatchDue();

        assertThat(outbox.delivered).isEmpty();
        assertThat(outbox.failed).containsExactly(new Failure(message.id(), "IllegalStateException", NOW.plusSeconds(5)));
        assertThat(metrics.get("claims.outbox.failed").tag("effect", message.effect()).counter().count()).isEqualTo(1);
    }

    @Test
    void losingDeliveryLeaseDoesNotCountEventAsDelivered() {
        var message = message("notify_member_approval");
        var outbox = new MemoryOutbox(List.of(message));
        outbox.allowDelivered = false;
        var metrics = new SimpleMeterRegistry();
        dispatcher(outbox, ignored -> {}, metrics, 100, 1).dispatchDue();
        assertThat(metrics.find("claims.outbox.delivered").counter()).isNull();
    }

    @Test
    void emptyBatchDoesNothingAndInvalidDispatcherSettingsFailFast() {
        var outbox = new MemoryOutbox(List.of());
        var metrics = new SimpleMeterRegistry();
        dispatcher(outbox, ignored -> { throw new AssertionError("no messages expected"); }, metrics, 1, 1).dispatchDue();
        assertThat(outbox.delivered).isEmpty();
        assertThatThrownBy(() -> dispatcher(outbox, ignored -> {}, metrics, 0, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be positive");
        assertThatThrownBy(() -> dispatcher(outbox, ignored -> {}, metrics, 1, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be positive");
    }

    @Test
    void deliversInsidePersistedParentTraceContextWhenTraceparentIsValid() {
        String traceparent = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";
        OutboxRepository.Message message = new OutboxRepository.Message(UUID.randomUUID(), UUID.randomUUID(),
                "notify", "{}", 1, NOW, traceparent);
        AtomicReference<Boolean> delivered = new AtomicReference<>(false);
        EffectDelivery delivery = ignored -> delivered.set(true);
        MemoryOutbox outbox = new MemoryOutbox(List.of(message));
        dispatcher(outbox, delivery, new SimpleMeterRegistry(), 10, 2).dispatchDue();
        assertThat(delivered.get()).isTrue();
        assertThat(outbox.delivered).containsExactly(message.id());
    }

    private OutboxDispatcher dispatcher(OutboxRepository outbox, EffectDelivery delivery,
            SimpleMeterRegistry metrics, long retryMillis, int batchSize) {
        return new OutboxDispatcher(outbox, delivery, Clock.fixed(NOW, ZoneOffset.UTC), metrics, retryMillis, batchSize);
    }

    private static OutboxRepository.Message message(String effect) {
        return new OutboxRepository.Message(UUID.randomUUID(), UUID.randomUUID(), effect, "{}", 1, NOW);
    }

    private static final class MemoryOutbox implements OutboxRepository {
        private final List<Message> messages;
        private Instant claimedAt;
        private int limit;
        private boolean allowDelivered = true;
        private final List<UUID> delivered = new ArrayList<>();
        private final List<Failure> failed = new ArrayList<>();

        private MemoryOutbox(List<Message> messages) { this.messages = messages; }
        @Override public void enqueue(Message message) { throw new UnsupportedOperationException(); }
        @Override public List<Message> claimDue(Instant now, int max) { claimedAt = now; limit = max; return messages; }
        @Override public boolean markDelivered(UUID id, Instant at) { if (allowDelivered) delivered.add(id); return allowDelivered; }
        @Override public boolean markFailed(UUID id, String error, Instant retryAt) { failed.add(new Failure(id, error, retryAt)); return true; }
    }

    private record Failure(UUID id, String error, Instant retryAt) { }
}
