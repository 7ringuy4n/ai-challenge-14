package com.papaya.claims.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.contract.Model.ClaimSnapshot;
import com.papaya.claims.contract.Model.CommandReceipt;
import com.papaya.claims.contract.Model.OutboxMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = false)
class ClaimPersistenceIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("claims")
            .withUsername("postgres")
            .withPassword("integration-test-password");

    private static PostgresPersistenceSupport.Harness harness;

    @BeforeAll
    static void setup() throws Exception {
        harness = PostgresPersistenceSupport.start(postgres);
    }

    private ClaimSnapshot claim(UUID id) {
        return new ClaimSnapshot(id, "member-001", "RECEIVED", new BigDecimal("100.00"),
                "USD", Map.of("proof", true), 0, 0, null, null);
    }

    @Test
    void persistsJsonbAndReadsClaim() {
        ClaimSnapshot claim = claim(UUID.randomUUID());

        harness.claims().insert(claim);

        assertThat(harness.claims().find(claim.id())).contains(claim);
    }

    @Test
    void locksClaimInsideTransactionAndCasChecksVersion() {
        ClaimSnapshot claim = claim(UUID.randomUUID());
        harness.claims().insert(claim);
        ClaimSnapshot updated = claim.updateFacts(Map.of("proof", false));

        Boolean updatedSuccessfully = harness.transactions().execute(status -> {
            assertThat(harness.claims().findForUpdate(claim.id())).contains(claim);
            return harness.claims().compareAndSet(updated, 0);
        });

        assertThat(updatedSuccessfully).isTrue();
        assertThat(harness.claims().compareAndSet(claim, 0)).isFalse();
        assertThat(harness.claims().find(claim.id()).orElseThrow().facts())
                .containsEntry("proof", false);
    }

    @Test
    void transactionRollbackRemovesClaim() {
        ClaimSnapshot claim = claim(UUID.randomUUID());

        assertThatThrownBy(() -> harness.transactions().executeWithoutResult(status -> {
            harness.claims().insert(claim);
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(harness.claims().find(claim.id())).isEmpty();
    }

    @Test
    void receiptIsUniqueByClaimUserAndIdempotencyKeyAndStoresSnapshot() {
        ClaimSnapshot claim = claim(UUID.randomUUID());
        harness.claims().insert(claim);
        CommandReceipt receipt = new CommandReceipt("sha256", claim);

        harness.commands().insert(claim.id(), "clerk", "key-1", receipt);

        assertThat(harness.commands().find(claim.id(), "clerk", "key-1")).contains(receipt);
        assertThatThrownBy(() -> harness.commands().insert(claim.id(), "clerk", "key-1", receipt))
                .hasMessageContaining("command_receipts");
        harness.commands().insert(claim.id(), "lead", "key-1", receipt);
    }

    @Test
    void paymentRequestIsUniquePerClaimAndOutboxRetries() {
        ClaimSnapshot claim = claim(UUID.randomUUID());
        harness.claims().insert(claim);
        harness.effects().createPaymentRequest(claim.id(), UUID.randomUUID());
        harness.effects().createPaymentRequest(claim.id(), UUID.randomUUID());

        assertThat(harness.effects().paymentRequestExists(claim.id())).isTrue();
        UUID outboxId = UUID.randomUUID();
        harness.effects().enqueue(List.of(
                new OutboxMessage(outboxId, claim.id(), "notify_approval", "{}", 0)));
        assertThat(harness.effects().pending()).extracting(OutboxMessage::id).contains(outboxId);

        harness.effects().markFailed(outboxId, "network", Instant.now().plusSeconds(30));

        assertThat(harness.effects().pending()).extracting(OutboxMessage::id).doesNotContain(outboxId);
    }
}
