package com.papaya.claims.claim.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.claim.domain.AuditRecord;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimFacts;
import com.papaya.claims.claim.domain.TransitionResult;
import com.papaya.claims.workflow.WorkflowOption;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiContractTest {
    @Test
    void mapsTransitionAuditAndWorkflowOptionsWithoutLosingRolesEffectsOrReplayStatus() {
        UUID id = UUID.randomUUID();
        Instant at = Instant.EPOCH;
        Claim claim = new Claim(id, "member-1", "CLOSED", new BigDecimal("10.00"), "USD", 1,
                new ClaimFacts(Map.of()), at, at);
        AuditRecord audit = new AuditRecord(UUID.randomUUID(), id, 1, at, "REJECTED", "CLOSED", "system", "system", "", "corr", "v1");
        TransitionResultView result = TransitionResultView.from(new TransitionResult(claim, audit, true));
        assertThat(result.replayed()).isTrue();
        assertThat(result.claim().id()).isEqualTo(id);
        assertThat(result.audit().sequence()).isEqualTo(1);

        WorkflowOptionView option = WorkflowOptionView.from(new WorkflowOption("close-rejected", "REJECTED", "CLOSED",
                Set.of("system"), false, List.of("appeal_period_expired"), List.of("archive_claim")));
        assertThat(option.authorizedRoles()).containsExactly("system");
        assertThat(option.failures()).containsExactly("appeal_period_expired");
        assertThat(option.effects()).containsExactly("archive_claim");

        assertThatThrownBy(() -> new TransitionResultView(null, result.audit(), false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TransitionResultView.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void apiErrorsCopyDetailsAndUseSafeEmptyDefaults() {
        var details = new java.util.ArrayList<>(List.of("field required"));
        ApiError error = new ApiError("BAD_REQUEST", "Invalid request", details, null);
        details.add("later");
        assertThat(error.details()).containsExactly("field required");
        assertThat(error.correlationId()).isEmpty();
        assertThat(new ApiError("BAD_REQUEST", "Invalid request", null, "c").details()).isEmpty();
        assertThatThrownBy(() -> new ApiError(" ", "message", List.of(), "c")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiError(null, "message", List.of(), "c")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiError("code", " ", List.of(), "c")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiError("code", null, List.of(), "c")).isInstanceOf(IllegalArgumentException.class);
    }
}
