package com.papaya.claims.claim.effects;

import com.papaya.claims.claim.domain.OutboxRepository;
import com.papaya.claims.claim.domain.PaymentRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Console mock that records event metadata only; workflow payloads can contain sensitive claim details. */
@Component
public class MockEffectDelivery implements EffectDelivery {
    private static final Logger log = LoggerFactory.getLogger(MockEffectDelivery.class);
    private final PaymentRepository payments;
    public MockEffectDelivery(PaymentRepository payments) { this.payments = payments; }
    @Override public void deliver(OutboxRepository.Message message) {
        if (message.effect().equals("trigger_payment_system")) {
            payments.confirm(message.claimId(), "MOCK-" + message.id());
        }
        SpanContext context = Span.current().getSpanContext();
        log.info("mock_effect_delivered eventId={} effect={} traceId={} spanId={}", message.id(), message.effect(),
                context.isValid() ? context.getTraceId() : null, context.isValid() ? context.getSpanId() : null);
    }
}
