package com.papaya.claims.claim.effects;

import com.papaya.claims.claim.domain.OutboxRepository;
import com.papaya.claims.common.web.Traceparent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final OpenTelemetry openTelemetry;

    @Autowired
    public OutboxDispatcher(OutboxRepository outbox, EffectDelivery delivery, Clock clock, MeterRegistry metrics,
            OpenTelemetry openTelemetry,
            @Value("${claims.outbox.retry-delay-ms:5000}") long retryDelayMs,
            @Value("${claims.outbox.batch-size:32}") int batchSize) {
        if (retryDelayMs < 1 || batchSize < 1) throw new IllegalArgumentException("Outbox retry delay and batch size must be positive");
        this.outbox = outbox; this.delivery = delivery; this.clock = clock;
        this.retryDelay = Duration.ofMillis(retryDelayMs); this.batchSize = batchSize; this.metrics = metrics;
        this.openTelemetry = openTelemetry;
    }

    public OutboxDispatcher(OutboxRepository outbox, EffectDelivery delivery, Clock clock, MeterRegistry metrics,
            long retryDelayMs, int batchSize) {
        this(outbox, delivery, clock, metrics, OpenTelemetry.noop(), retryDelayMs, batchSize);
    }

    private void deliverWithTrace(OutboxRepository.Message message) {
        Context parent = Traceparent.parent(message.traceparent()).orElse(null);
        if (parent == null) {
            delivery.deliver(message);
            return;
        }
        Span span = openTelemetry.getTracer("claims-workflow-outbox")
                .spanBuilder("claims.outbox.delivery")
                .setParent(parent)
                .setAttribute("claims.outbox.effect", message.effect())
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            delivery.deliver(message);
        } catch (RuntimeException failure) {
            span.recordException(failure);
            span.setStatus(StatusCode.ERROR);
            throw failure;
        } finally {
            span.end();
        }
    }

    @Scheduled(fixedDelayString = "${claims.outbox.poll-interval-ms:1000}")
    @Transactional
    public void dispatchDue() {
        List<OutboxRepository.Message> batch = outbox.claimDue(clock.instant(), batchSize);
        for (OutboxRepository.Message message : batch) {
            try {
                deliverWithTrace(message);
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
