package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.claim.domain.OutboxRepository;
import com.papaya.claims.claim.domain.PaymentRepository;
import com.papaya.claims.claim.domain.mapper.EffectsMapper;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EffectsRepositoryTest {
    @Test
    void mapsOutboxRowsAndReportsBothDeliveryUpdateOutcomes() {
        UUID id = UUID.randomUUID(), claimId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-17T12:00:00Z");
        OutboxRow selected = new OutboxRow();
        selected.id = id; selected.claimId = claimId; selected.effect = "archive_claim";
        selected.payloadJson = "{}"; selected.attempts = 2; selected.availableAt = now;
        Object[] outcome = {1};
        Object[] captured = {null};
        EffectsMapper mapper = mapper((method, args) -> switch (method) {
            case "claimDue" -> List.of(selected);
            case "delivered", "failed" -> outcome[0];
            case "enqueue" -> { captured[0] = args[0]; yield 1; }
            default -> null;
        });
        var repository = new MyBatisOutboxRepository(mapper, 250);
        OutboxRepository.Message message = new OutboxRepository.Message(id, claimId, "archive_claim", "{}", 2, now);
        repository.enqueue(message);
        assertThat(captured[0]).isInstanceOf(OutboxRow.class);
        assertThat(repository.claimDue(now, 5)).containsExactly(message);
        assertThat(repository.markDelivered(id, now)).isTrue();
        assertThat(repository.markFailed(id, "safe", now.plusSeconds(1))).isTrue();
        outcome[0] = 0;
        assertThat(repository.markDelivered(id, now)).isFalse();
        assertThat(repository.markFailed(id, "safe", now.plusSeconds(1))).isFalse();
        assertThatThrownBy(() -> repository.claimDue(now, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MyBatisOutboxRepository(mapper, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsPaymentInsertConfirmationExistenceAndConditionalConfirmation() {
        UUID claimId = UUID.randomUUID();
        Object[] row = {null};
        Object[] exists = {false};
        Object[] update = {0};
        EffectsMapper mapper = mapper((method, args) -> switch (method) {
            case "createPayment" -> { row[0] = args[0]; yield 1; }
            case "paymentConfirmation" -> row[0];
            case "paymentExists" -> exists[0];
            case "confirmPayment" -> update[0];
            default -> null;
        });
        var repository = new MyBatisPaymentRepository(mapper);
        repository.create(claimId, UUID.randomUUID(), "USD", new BigDecimal("12.34"));
        assertThat(row[0]).isInstanceOf(PaymentRow.class);
        row[0] = null;
        assertThat(repository.exists(claimId)).isFalse();
        exists[0] = true;
        assertThat(repository.exists(claimId)).isTrue();
        assertThat(repository.confirmation(claimId)).isEmpty();

        PaymentRow confirmation = new PaymentRow();
        confirmation.reference = "REF-1";
        confirmation.confirmedAt = Instant.EPOCH;
        row[0] = confirmation;
        assertThat(repository.confirmation(claimId)).contains(new PaymentRepository.Confirmation("REF-1", Instant.EPOCH));
        update[0] = 1;
        assertThat(repository.confirm(claimId, "REF-1")).isTrue();
        update[0] = 0;
        assertThat(repository.confirm(claimId, "REF-1")).isFalse();
    }

    @FunctionalInterface private interface Invocation { Object call(String method, Object[] args) throws Exception; }
    private static EffectsMapper mapper(Invocation invocation) {
        return (EffectsMapper) Proxy.newProxyInstance(EffectsMapper.class.getClassLoader(), new Class<?>[]{EffectsMapper.class},
                (proxy, method, args) -> invocation.call(method.getName(), args == null ? new Object[0] : args));
    }
}
