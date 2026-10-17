package com.papaya.claims.claim.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClaimViewTest {
    @Test
    void copiesNestedFactValuesOnConstruction() {
        var nested = new ArrayList<>(List.of("receipt"));
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("documents", nested);
        var view = new ClaimView(UUID.randomUUID(), "SUBMITTED", new java.math.BigDecimal("100.00"), "USD",
                0, facts, Instant.EPOCH, Instant.EPOCH);
        nested.add("external mutation");

        assertThat(view.facts()).containsOnlyKeys("documents");
        assertThat(view.facts().get("documents")).isEqualTo(List.of("receipt"));
        assertThatThrownBy(() -> ((List<?>) view.facts().get("documents")).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsPolicyLimitOutsideNumericEighteenTwo() {
        assertThatThrownBy(() -> new ClaimView(UUID.randomUUID(), "SUBMITTED", new java.math.BigDecimal("1.001"),
                "USD", 0, Map.of(), Instant.EPOCH, Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Policy limit must be nonnegative and fit numeric(18,2)");
        assertThatThrownBy(() -> new ClaimView(UUID.randomUUID(), "SUBMITTED", new java.math.BigDecimal("10000000000000000"),
                "USD", 0, Map.of(), Instant.EPOCH, Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Policy limit must be nonnegative and fit numeric(18,2)");
    }

    @Test
    void rejectsInvalidRequiredIdentityStateCurrencyVersionFactsAndTimestamps() {
        Instant at = Instant.EPOCH;
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new ClaimView(null, "SUBMITTED", java.math.BigDecimal.ONE, "USD", 0, Map.of(), at, at))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClaimView(id, " ", java.math.BigDecimal.ONE, "USD", 0, Map.of(), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", null, "USD", 0, Map.of(), at, at))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", new java.math.BigDecimal("-0.01"), "USD", 0, Map.of(), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", java.math.BigDecimal.ONE, null, 0, Map.of(), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", java.math.BigDecimal.ONE, "usd", 0, Map.of(), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", java.math.BigDecimal.ONE, "US", 0, Map.of(), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", java.math.BigDecimal.ONE, "USD", -1, Map.of(), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", java.math.BigDecimal.ONE, "USD", 0, null, at, at))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", java.math.BigDecimal.ONE, "USD", 0, Map.of(), null, at))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClaimView(id, "SUBMITTED", java.math.BigDecimal.ONE, "USD", 0, Map.of(), at, null))
                .isInstanceOf(NullPointerException.class);
    }
}
