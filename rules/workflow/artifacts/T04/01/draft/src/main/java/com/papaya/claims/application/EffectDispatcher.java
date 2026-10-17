package com.papaya.claims.application;

import com.papaya.claims.contract.EffectRepository;
import com.papaya.claims.contract.Model.OutboxMessage;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** At-least-once console delivery with persisted retry state. */
@Component
public final class EffectDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(EffectDispatcher.class);
    private final EffectRepository repository; private final MockEffectFactory factory; private final Clock clock;
    public EffectDispatcher(EffectRepository repository, MockEffectFactory factory, Clock clock) { this.repository = repository; this.factory = factory; this.clock = clock; }
    public void dispatch(List<OutboxMessage> messages) { messages.forEach(this::deliver); }
    @Scheduled(fixedDelayString = "${claims.outbox-poll-ms:5000}")
    public void retryPending() { repository.pending().forEach(this::deliver); }
    private void deliver(OutboxMessage message) {
        try { factory.deliver(message); repository.markDelivered(message.id()); }
        catch (RuntimeException failure) {
            int seconds = (int)Math.min(3600, 1L << Math.min(message.attempts(), 10));
            try { repository.markFailed(message.id(), failure.getClass().getSimpleName(), clock.instant().plus(Duration.ofSeconds(seconds))); }
            catch (RuntimeException persistenceFailure) { LOG.error("Unable to persist outbox retry state id={}", message.id(), persistenceFailure); }
            LOG.warn("Mock effect delivery failed id={} attempt={}", message.id(), message.attempts() + 1);
        }
    }
}
