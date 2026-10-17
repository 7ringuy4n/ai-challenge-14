package com.papaya.claims.claim.effects;

import static org.assertj.core.api.Assertions.assertThat;

import com.papaya.claims.claim.domain.OutboxRepository;
import com.papaya.claims.claim.domain.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockEffectDeliveryTest {
    @Test
    void paymentDispatchConfirmsPaymentUsingEventIdAndOtherEffectsDoNotTouchPayments() {
        var payments = new RecordingPayments();
        var delivery = new MockEffectDelivery(payments);
        UUID claimId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-17T02:00:00Z");

        delivery.deliver(new OutboxRepository.Message(eventId, claimId, "trigger_payment_system", "sensitive", 1, at));
        delivery.deliver(new OutboxRepository.Message(UUID.randomUUID(), claimId, "notify_member_approval", "sensitive", 1, at));

        assertThat(payments.confirmations).containsExactly(new ConfirmationCall(claimId, "MOCK-" + eventId));
    }

    private static final class RecordingPayments implements PaymentRepository {
        private final List<ConfirmationCall> confirmations = new ArrayList<>();
        @Override public void create(UUID claimId, UUID requestId, String currency, BigDecimal amount) { }
        @Override public boolean exists(UUID claimId) { return false; }
        @Override public Optional<Confirmation> confirmation(UUID claimId) { return Optional.empty(); }
        @Override public boolean confirm(UUID claimId, String reference) {
            confirmations.add(new ConfirmationCall(claimId, reference));
            return true;
        }
    }

    private record ConfirmationCall(UUID claimId, String reference) { }
}
