package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.papaya.claims.claim.domain.*;
import com.papaya.claims.claim.domain.IdempotencyRepository.Receipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionRollbackIT extends PersistenceITSupport {
    @Test
    void oneSpringTransactionRollsBackClaimAuditReceiptPaymentAndOutboxTogether() {
        UUID claimId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(runtimeDataSource()));

        tx.executeWithoutResult(status -> {
            var claimRepo = new MyBatisClaimRepository(claims(), JSON);
            var auditRepo = new MyBatisClaimAuditRepository(audits());
            var receiptRepo = new MyBatisIdempotencyRepository(receipts(), JSON);
            var paymentRepo = new MyBatisPaymentRepository(effects());
            var outboxRepo = new MyBatisOutboxRepository(effects(), 30_000);

            claimRepo.insert(new ClaimAggregate(new Claim(claimId, "member-rollback", "submitted",
                    new BigDecimal("125.00"), "USD", 0, new ClaimFacts(Map.of("source", "transaction-test")),
                    now, now), ClaimWorkflowData.empty()));
            auditRepo.append(new AuditRecord(UUID.randomUUID(), claimId, 0, now, null, "submitted",
                    "member-rollback", "member", "created", "rollback-correlation", "workflow-v1"));
            new MyBatisSystemOperationAuditRepository(systemAudits()).append(new SystemOperationAuditRecord(
                    UUID.randomUUID(), "claim.create", "claim", claimId.toString(), "member-rollback",
                    "rollback-correlation", "success", null, now));
            receiptRepo.insert(new Receipt(receiptId, "member-rollback", "claim.create", "rollback-key",
                    "a".repeat(64), "{\"claimId\":\"" + claimId + "\"}"));
            paymentRepo.create(claimId, requestId, "USD", new BigDecimal("125.00"));
            outboxRepo.enqueue(new OutboxRepository.Message(outboxId, claimId, "payment.request",
                    "{\"requestId\":\"" + requestId + "\"}", 0, now));
            status.setRollbackOnly();
        });

        try (var connection = runtime(); var statement = connection.createStatement()) {
            assertThat(claims().find(claimId)).isNull();
            assertThat(audits().count(claimId)).isZero();
            assertThat(receipts().find("member-rollback", "claim.create", "rollback-key")).isNull();
            assertThat(effects().paymentExists(claimId)).isFalse();
            try (var result = statement.executeQuery("SELECT count(*) FROM system_operation_audit WHERE resource_id='" + claimId + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isZero();
            }
            try (var result = statement.executeQuery("SELECT count(*) FROM local_outbox WHERE id='" + outboxId + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isZero();
            }
        } catch (Exception e) {
            throw new AssertionError("Unable to verify rolled-back rows", e);
        }
    }
}
