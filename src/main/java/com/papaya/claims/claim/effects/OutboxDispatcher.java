package com.papaya.claims.claim.effects;

import com.papaya.claims.claim.domain.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final OutboxRepository outbox;
    private final EffectDelivery delivery;
    private final Clock clock;
    private final Duration retryDelay;
    private final int batchSize;
    private final MeterRegistry metrics;

    public OutboxDispatcher(OutboxRepository outbox, EffectDelivery delivery, Clock clock, MeterRegistry metrics,
            @Value("${claims.outbox.retry-delay-ms:5000}") long retryDelayMs,
            @Value("${claims.outbox.batch-size:32}") int batchSize) {
        if (retryDelayMs < 1 || batchSize < 1) throw new IllegalArgumentException("Outbox retry delay and batch size must be positive");
        this.outbox = outbox; this.delivery = delivery; this.clock = clock;
        this.retryDelay = Duration.ofMillis(retryDelayMs); this.batchSize = batchSize; this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${claims.outbox.poll-interval-ms:1000}")
    @Transactional
    public void dispatchDue() {
        List<OutboxRepository.Message> batch = outbox.claimDue(clock.instant(), batchSize);
        for (OutboxRepository.Message message : batch) {
            try {
                delivery.deliver(message);
                if (outbox.markDelivered(message.id(), clock.instant())) {
                    metrics.counter("claims.outbox.delivered", "effect", message.effect()).increment();
                }
            } catch (RuntimeException failure) {
                outbox.markFailed(message.id(), failure.getClass().getSimpleName(), clock.instant().plus(retryDelay));
                metrics.counter("claims.outbox.failed", "effect", message.effect()).increment();
                log.warn("mock_effect_delivery_failed eventId={} effect={} exceptionType={}",
                        message.id(), message.effect(), failure.getClass().getSimpleName());
            }
        }
    }
}
