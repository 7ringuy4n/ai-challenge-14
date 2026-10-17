package com.papaya.claims.claim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.papaya.claims.workflow.WorkflowEvidence;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainContractTest {
    @Test
    void factsDefensivelyCopyNestedJsonCollectionsAndPreserveDecimalPrecision() {
        List<Object> nested = new ArrayList<>(List.of(new BigDecimal("0.10")));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("documents", nested);
        ClaimFacts facts = new ClaimFacts(input);
        nested.add("late mutation");
        input.put("new", true);

        assertThat(facts.values()).containsOnlyKeys("documents");
        assertThat(facts.values().get("documents")).isEqualTo(List.of(new BigDecimal("0.10")));
        assertThatThrownBy(() -> facts.values().put("x", true)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ((List<?>) facts.values().get("documents")).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void actorCopiesRolesAndRequiresProtectedSystemRole() {
        var roles = new java.util.HashSet<>(Set.of("assessor"));
        Actor actor = new Actor("u-1", roles, false);
        roles.add("finance");
        assertThat(actor.roles()).containsExactly("assessor");
        assertThatThrownBy(() -> new Actor("system-job", Set.of(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("System actor must hold the protected system role");
    }

    @Test
    void pageBoundsAndOverflowAreExplicit() {
        assertThat(new PageRequest(2, 25).offset()).isEqualTo(50);
        assertThatThrownBy(() -> new PageRequest(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(0, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new PageRequest(Integer.MAX_VALUE, 100).offset()).isEqualTo(214_748_364_700L);
    }

    @Test
    void validatesClaimAmountsCurrencyVersionAndRequiredTimestamps() {
        Instant at = Instant.parse("2026-10-17T12:00:00Z");
        assertThatThrownBy(() -> claim(new BigDecimal("-0.01"), "USD", 0, at, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Policy limit");
        assertThatThrownBy(() -> claim(new BigDecimal("1.001"), "USD", 0, at, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Policy limit");
        assertThatThrownBy(() -> claim(new BigDecimal("10000000000000000.00"), "USD", 0, at, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Policy limit");
        assertThatThrownBy(() -> claim(BigDecimal.ONE, "usd", 0, at, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Currency");
        assertThatThrownBy(() -> claim(BigDecimal.ONE, "US", 0, at, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Currency");
        assertThatThrownBy(() -> claim(BigDecimal.ONE, "USD", -1, at, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("version");
        assertThatThrownBy(() -> claim(BigDecimal.ONE, "USD", 0, null, at)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> claim(BigDecimal.ONE, "USD", 0, at, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Claim(null, "member", "SUBMITTED", BigDecimal.ONE, "USD", 0, new ClaimFacts(Map.of()), at, at))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Claim(UUID.randomUUID(), " ", "SUBMITTED", BigDecimal.ONE, "USD", 0, new ClaimFacts(Map.of()), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Claim(UUID.randomUUID(), "member", " ", BigDecimal.ONE, "USD", 0, new ClaimFacts(Map.of()), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Claim(UUID.randomUUID(), "member", "SUBMITTED", null, "USD", 0, new ClaimFacts(Map.of()), at, at))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Claim(UUID.randomUUID(), "member", "SUBMITTED", BigDecimal.ONE, null, 0, new ClaimFacts(Map.of()), at, at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Claim(UUID.randomUUID(), "member", "SUBMITTED", BigDecimal.ONE, "USD", 0, null, at, at))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesAuditTransitionAndOperationalAuditContracts() {
        UUID id = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-17T12:00:00Z");
        assertThatThrownBy(() -> new AuditRecord(id, id, -1, at, null, "SUBMITTED", "member-1", "MEMBER", "", "c", "v1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sequence");
        assertThatThrownBy(() -> new AuditRecord(id, id, 0, at, null, " ", "member-1", "MEMBER", "", "c", "v1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("target state");
        assertThatThrownBy(() -> new TransitionCommand(" ", 0, null, null)).isInstanceOf(IllegalArgumentException.class);
        var command = new TransitionCommand("SUBMITTED", 0, null, null);
        assertThat(command.reason()).isEmpty();
        assertThat(command.suppliedFacts().values()).isEmpty();
        assertThatThrownBy(() -> new SystemOperationAuditRecord(id, " ", "claim", null, "member", "c", "FAILED", null, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("action");
        assertThatThrownBy(() -> new SystemOperationAuditRecord(id, "read", null, null, "member", "c", "FAILED", null, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("resource");
        assertThatThrownBy(() -> new SystemOperationAuditRecord(id, "read", "claim", null, " ", "c", "FAILED", null, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("actorId");
        assertThatThrownBy(() -> new SystemOperationAuditRecord(id, "read", "claim", null, "member", "c", " ", null, at))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("outcome");
        assertThatThrownBy(() -> new SystemOperationAuditRecord(id, "read", "claim", null, "member", null, "FAILED", null, at))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditRecord(id, id, 0, at, null, "SUBMITTED", " ", "MEMBER", "", "c", "v1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("actor");
        assertThatThrownBy(() -> new AuditRecord(id, id, 0, at, null, "SUBMITTED", "member", null, "", "c", "v1"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Actor(" ", Set.of(), false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Actor("actor", null, false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Actor("system", Set.of(), true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TransitionCommand("APPROVED", -1, "", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WorkflowEvidence(-1, null, null, null, null, null, false, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void factsAcceptJsonNumbersButRejectInvalidKeysAndUnsupportedObjects() {
        assertThat(new ClaimFacts(Map.of("number", 1.5d)).values().get("number"))
                .isEqualTo(new BigDecimal("1.5"));
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("null", null);
        json.put("integer", Integer.valueOf(2));
        json.put("long", Long.valueOf(3));
        json.put("double", Double.valueOf(4.25));
        json.put("nested", List.of(Map.of("flag", true), 5.5f));
        ClaimFacts copied = new ClaimFacts(json);
        assertThat(copied.values().get("null")).isNull();
        assertThat(copied.values().get("integer")).isEqualTo(2);
        assertThat(copied.values().get("long")).isEqualTo(3L);
        assertThat(copied.values().get("double")).isEqualTo(new BigDecimal("4.25"));
        assertThat(copied.values().get("nested")).isEqualTo(List.of(Map.of("flag", true), new BigDecimal("5.5")));
        assertThatThrownBy(() -> new ClaimFacts(Map.of("key", List.of(new Object())))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimFacts(Map.of(" ", true))).isInstanceOf(IllegalArgumentException.class);
        Map<Object, Object> invalid = new LinkedHashMap<>();
        invalid.put(1, "value");
        assertThatThrownBy(() -> new ClaimFacts(Map.of("nested", invalid))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimFacts(Map.of("value", new Object())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsupported");
    }

    @Test
    void enforcesRequiredValuesAcrossDomainRecords() {
        Instant at = Instant.EPOCH;
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new CreateClaimCommand(null, "USD", new ClaimFacts(Map.of()))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateClaimCommand(BigDecimal.ONE, null, new ClaimFacts(Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateClaimCommand(BigDecimal.ONE, "USD", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateClaimCommand(new BigDecimal("-0.01"), "USD", new ClaimFacts(Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateClaimCommand(new BigDecimal("1.001"), "USD", new ClaimFacts(Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateClaimCommand(new BigDecimal("10000000000000000"), "USD", new ClaimFacts(Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateClaimCommand(BigDecimal.ONE, "US", new ClaimFacts(Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdateFactsCommand(-1, new ClaimFacts(Map.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UpdateFactsCommand(0, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PageResult<>(null, 0, new PageRequest(0, 1))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PageResult<>(List.of(), -1, new PageRequest(0, 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResult<>(List.of("a", "b"), 2, new PageRequest(0, 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClaimWorkflowData(-1, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ClaimWorkflowData(2, at.plusNanos(900), at.plusNanos(800), at.plusNanos(700), at.plusNanos(600), at.plusNanos(500)))
                .satisfies(data -> {
                    assertThat(data.lastRequested()).contains(at.plusNanos(900).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
                    assertThat(data.lastInformationReceivedAt()).isEqualTo(at.plusNanos(800).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
                    assertThat(data.assessmentStartedAt()).isEqualTo(at.plusNanos(700).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
                    assertThat(data.rejectedAt()).isEqualTo(at.plusNanos(600).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
                    assertThat(data.memberAcknowledgedRejectionAt()).isEqualTo(at.plusNanos(500).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
                });
        assertThat(ClaimWorkflowData.empty().lastRequested()).isEmpty();
    }

    private static Claim claim(BigDecimal limit, String currency, long version, Instant created, Instant updated) {
        return new Claim(UUID.randomUUID(), "member-1", "SUBMITTED", limit, currency, version,
                new ClaimFacts(Map.of()), created, updated);
    }
}
