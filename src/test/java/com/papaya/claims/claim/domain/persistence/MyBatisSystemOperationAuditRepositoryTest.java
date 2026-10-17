package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import com.papaya.claims.claim.domain.SystemOperationAuditRecord;
import com.papaya.claims.claim.domain.mapper.SystemAuditMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MyBatisSystemOperationAuditRepositoryTest {
    @Test
    void mapsEveryAuditFieldToAppendOnlyMapper() {
        CapturingMapper mapper = new CapturingMapper();
        var repository = new MyBatisSystemOperationAuditRepository(mapper);
        var record = new SystemOperationAuditRecord(UUID.randomUUID(), "claim.create", "claim", "claim-1",
                "member-1", "correlation-1", "success", null, Instant.parse("2026-10-17T02:00:00Z"));

        repository.append(record);

        assertThat(mapper.row).isNotNull().satisfies(row -> {
            assertThat(row.id).isEqualTo(record.id());
            assertThat(row.action).isEqualTo(record.action());
            assertThat(row.resource).isEqualTo(record.resource());
            assertThat(row.resourceId).isEqualTo(record.resourceId());
            assertThat(row.actorId).isEqualTo(record.actorId());
            assertThat(row.correlationId).isEqualTo(record.correlationId());
            assertThat(row.outcome).isEqualTo(record.outcome());
            assertThat(row.errorType).isNull();
            assertThat(row.occurredAt).isEqualTo(record.occurredAt());
        });
    }

    private static final class CapturingMapper implements SystemAuditMapper {
        private SystemAuditRow row;
        @Override public int append(SystemAuditRow value) { row = value; return 1; }
    }
}
