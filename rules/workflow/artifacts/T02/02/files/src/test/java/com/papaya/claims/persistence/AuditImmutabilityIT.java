package com.papaya.claims.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.contract.Model.AuditEntry;
import com.papaya.claims.contract.Model.ClaimSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = false)
class AuditImmutabilityIT {
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

    @Test
    void runtimeCanInsertAndReadAuditButCannotUpdateOrDelete() throws Exception {
        ClaimSnapshot claim = new ClaimSnapshot(UUID.randomUUID(), "member-001", "RECEIVED",
                new BigDecimal("50.00"), "USD", Map.of(), 0, 0, null, null);
        harness.claims().insert(claim);
        AuditEntry entry = audit(claim.id(), "trace-1");

        harness.audits().append(entry);

        assertThat(harness.audits().findByClaimId(claim.id())).containsExactly(entry);
        try (var connection = harness.dataSource().getConnection();
             var statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE claim_audit SET notes='tampered' WHERE audit_id='" + entry.auditId() + "'"))
                    .hasMessageContaining("permission denied");
            assertThatThrownBy(() -> statement.executeUpdate(
                    "DELETE FROM claim_audit WHERE audit_id='" + entry.auditId() + "'"))
                    .hasMessageContaining("permission denied");
        }
    }

    @Test
    void auditCannotBeMutatedEvenByTableOwnerBecauseTriggerDefendsIt() throws Exception {
        ClaimSnapshot claim = new ClaimSnapshot(UUID.randomUUID(), "member-001", "RECEIVED",
                new BigDecimal("50.00"), "USD", Map.of(), 0, 0, null, null);
        harness.claims().insert(claim);
        AuditEntry entry = audit(claim.id(), "trace-2");
        harness.audits().append(entry);

        try (var connection = java.sql.DriverManager.getConnection(
                        postgres.getJdbcUrl(), "claims_owner", "integration-test-password");
             var statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE claim_audit SET notes='tampered' WHERE audit_id='" + entry.auditId() + "'"))
                    .hasMessageContaining("claim audit is immutable");
        }
    }

    private AuditEntry audit(UUID claimId, String traceId) {
        return new AuditEntry(UUID.randomUUID(), claimId, 1, Instant.now(), null, "RECEIVED",
                "clerk", "document_clerk", "created", "v1", traceId);
    }
}
