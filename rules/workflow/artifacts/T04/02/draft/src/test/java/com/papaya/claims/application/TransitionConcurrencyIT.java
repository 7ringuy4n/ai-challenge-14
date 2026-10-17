package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import com.papaya.claims.contract.Model.*;
import com.papaya.claims.persistence.PostgresPersistenceSupport;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class TransitionConcurrencyIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Test void rowLockAndVersionAllowOnlyOneConcurrentCommand() throws Exception {
        var h = PostgresPersistenceSupport.start(POSTGRES); UUID id = UUID.randomUUID();
        ClaimSnapshot claim = new ClaimSnapshot(id, "member-001", "APPROVED", new BigDecimal("25.00"), "USD", Map.of("paymentRequestCreated", true), 0, 0, null, null);
        h.transactions().executeWithoutResult(s -> { h.claims().insert(claim); h.effects().createPaymentRequest(id, UUID.randomUUID()); });
        ClaimService service = TransitionAtomicityIT.service(h, h.audits(), h.effects());
        var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> attempt = () -> { start.await(); try {
            service.transition(new Actor("finance", "finance"), id, new TransitionCommand("PAYMENT_INITIATED", 0, UUID.randomUUID().toString(), "start payment")); return true;
        } catch (RuntimeException expectedLoser) { return false; } };
        Future<Boolean> one = pool.submit(attempt); Future<Boolean> two = pool.submit(attempt); start.countDown();
        boolean won = one.get(30, TimeUnit.SECONDS) ^ two.get(30, TimeUnit.SECONDS); pool.shutdownNow();
        assertThat(won).isTrue(); assertThat(h.claims().find(id).orElseThrow().version()).isEqualTo(1);
        assertThat(h.audits().findByClaimId(id)).hasSize(1); assertThat(h.commands().find(id, "finance", "unused")).isEmpty();
    }
}
