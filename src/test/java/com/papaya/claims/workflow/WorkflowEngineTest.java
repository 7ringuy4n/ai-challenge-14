package com.papaya.claims.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimFacts;
import com.papaya.claims.claim.domain.TransitionCommand;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowEngineTest {
    private WorkflowEngine engine;
    private WorkflowDefinition definition;
    private final Instant now = Instant.parse("2026-10-17T12:00:00Z");

    @BeforeEach
    void loadEngine() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/workflow.yml")) {
            definition = new WorkflowConfigLoader().load(input);
        }
        engine = new WorkflowEngine(definition, new GuardEvaluator());
    }

    @Test
    void evaluatesConfiguredEdgeAndReportsRoleGuardAndInvalidEdgeFailures() {
        Claim claim = claim("SUBMITTED", Map.of("documents", Map.of("id-card", Map.of(
                "required", true, "present", true, "valid", true))));
        var allowed = engine.evaluate(context(claim, Set.of("document_clerk"), "DOCUMENTS_VERIFIED", "", WorkflowEvidence.empty()));
        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.transitionId()).isEqualTo("verify-documents");

        var forbidden = engine.evaluate(context(claim, Set.of("assessor"), "DOCUMENTS_VERIFIED", "", WorkflowEvidence.empty()));
        assertThat(forbidden.failureCode()).isEqualTo("ROLE_FORBIDDEN");
        var invalid = engine.evaluate(context(claim, Set.of("document_clerk"), "CLOSED", "", WorkflowEvidence.empty()));
        assertThat(invalid.failureCode()).isEqualTo("INVALID_EDGE");
        assertThat(invalid.from()).isEqualTo("SUBMITTED");
        assertThat(invalid.to()).isEqualTo("CLOSED");
        assertThat(invalid.failureMessage()).contains("SUBMITTED", "CLOSED", "verify-documents -> DOCUMENTS_VERIFIED");
    }

    @Test
    void allowsThirdInformationRequestAndRejectsFourthWithExactConfiguredMessage() {
        Claim claim = claim("UNDER_ASSESSMENT", Map.of());
        var third = engine.evaluate(context(claim, Set.of("assessor"), "PENDING_INFO", "Please send the invoice", evidence(2)));
        assertThat(third.allowed()).isTrue();
        var fourth = engine.evaluate(context(claim, Set.of("assessor"), "PENDING_INFO", "Please send the invoice", evidence(3)));
        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.failureCode()).isEqualTo("CYCLE_LIMIT");
        assertThat(fourth.failureMessage()).isEqualTo(definition.cyclePolicy().errorMessage());
    }

    @Test
    void availableTransitionsIncludeAuthorizationAndGuardFailures() {
        Claim claim = claim("UNDER_ASSESSMENT", Map.of());
        var options = engine.available(context(claim, Set.of("finance"), "APPROVED", "", evidence(0)));
        assertThat(options).hasSize(3);
        assertThat(options).allSatisfy(option -> assertThat(option.available()).isFalse());
        assertThat(options).anySatisfy(option -> {
            assertThat(option.transitionId()).isEqualTo("approve-claim");
            assertThat(option.failures()).contains("ROLE_FORBIDDEN", "assessment_report_complete", "amount_within_policy_limit");
        });
    }

    @Test
    void usesConfiguredCycleTransitionIdRatherThanAStateSpecificBranch() {
        var changed = new WorkflowDefinition(definition.version(), definition.initialState(), definition.states(),
                definition.transitions().stream().map(t -> t.id().equals("request-information")
                        ? new WorkflowDefinition.Transition("different-cycle-id", t.from(), t.to(), t.roles(), t.guards(), t.effects()) : t).toList(),
                definition.guardMessages(), definition.effects(), new WorkflowDefinition.CyclePolicy(
                        "different-cycle-id", "informationRequests", 1, "cycle stopped"));
        var changedEngine = new WorkflowEngine(changed, new GuardEvaluator());
        var denied = changedEngine.evaluate(context(claim("UNDER_ASSESSMENT", Map.of()), Set.of("assessor"),
                "PENDING_INFO", "More information required", evidence(1)));
        assertThat(denied.failureCode()).isEqualTo("CYCLE_LIMIT");
        assertThat(denied.failureMessage()).isEqualTo("cycle stopped");
    }

    @Test
    void resolvesEffectIdsThroughStrategiesIntoPureTypedIntents() {
        Claim claim = claim("UNDER_ASSESSMENT", Map.of(
                "assessmentReport", Map.of("complete", true),
                "requestedAmount", new BigDecimal("9.25")));
        var decision = engine.evaluate(context(claim, Set.of("assessor"), "APPROVED", "", evidence(0)));
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.effects()).extracting(EffectIntent::id)
                .containsExactly("notify_member_approval", "create_payment_request");
        assertThat(decision.effects().get(1).payload()).containsEntry("currency", "USD")
                .containsEntry("amount", new BigDecimal("9.25"));
    }

    @Test
    void rejectsUnknownEffectCatalogStrategiesWhenEngineIsConstructed() {
        var invalid = new WorkflowDefinition(definition.version(), definition.initialState(), definition.states(),
                definition.transitions(), definition.guardMessages(), Map.of("missing", "description"), definition.cyclePolicy());
        assertThatThrownBy(() -> new WorkflowEngine(invalid, new GuardEvaluator()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workflow definition contains an unregistered effect strategy");
    }

    @Test
    void rejectsBlankEffectIdsAndNullPayloads() {
        assertThatThrownBy(() -> new EffectIntent(null, Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EffectIntent(" ", Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EffectIntent("effect", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WorkflowEvaluationContext(null, new Actor("a", Set.of(), false),
                new TransitionCommand("CLOSED", 0, "", null), WorkflowEvidence.empty(), now, Duration.ofDays(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WorkflowEvaluationContext(claim("SUBMITTED", Map.of()), null,
                new TransitionCommand("CLOSED", 0, "", null), WorkflowEvidence.empty(), now, Duration.ofDays(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WorkflowEvaluationContext(claim("SUBMITTED", Map.of()), new Actor("a", Set.of(), false), null,
                WorkflowEvidence.empty(), now, Duration.ofDays(1))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WorkflowEvaluationContext(claim("SUBMITTED", Map.of()), new Actor("a", Set.of(), false),
                new TransitionCommand("CLOSED", 0, "", null), null, now, Duration.ofDays(1))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WorkflowEvaluationContext(claim("SUBMITTED", Map.of()), new Actor("a", Set.of(), false),
                new TransitionCommand("CLOSED", 0, "", null), WorkflowEvidence.empty(), null, Duration.ofDays(1)))
                .isInstanceOf(NullPointerException.class);
        Instant precise = now.plusNanos(999);
        assertThat(new WorkflowEvaluationContext(claim("SUBMITTED", Map.of()), new Actor("a", Set.of(), false),
                new TransitionCommand("CLOSED", 0, "", null), WorkflowEvidence.empty(), precise, Duration.ofDays(1)).now())
                .isEqualTo(precise.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        assertThat(new EffectRegistry().supports("missing")).isFalse();
        assertThatThrownBy(() -> new EffectRegistry().resolve("missing", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidEdgeWithNoOutgoingTransitionsReportsNone() {
        var decision = engine.evaluate(context(claim("CLOSED", Map.of()), Set.of("system"), "SUBMITTED", "", evidence(0)));
        assertThat(decision.failureCode()).isEqualTo("INVALID_EDGE");
        assertThat(decision.failureMessage()).contains("Available transitions: none");
        assertThat(decision.availableTransitions()).isEmpty();
    }

    @Test
    void plansEffectsForRejectionInformationPaymentAndArchiveEdges() {
        Claim assessment = claim("UNDER_ASSESSMENT", Map.of("assessmentReport", Map.of("complete", true),
                "requestedAmount", new BigDecimal("5.00")));
        assertThat(engine.evaluate(context(assessment, Set.of("assessor"), "REJECTED", "Insufficient evidence", evidence(0)))
                .effects()).extracting(EffectIntent::id).containsExactly("notify_member_rejection");
        assertThat(engine.evaluate(context(assessment, Set.of("assessor"), "PENDING_INFO", "Need invoice", evidence(0)))
                .effects()).extracting(EffectIntent::id).containsExactly("notify_information_request");
        WorkflowEvidence confirmed = new WorkflowEvidence(0, null, null, null, null, null,
                true, "REF-10", now);
        Claim approved = claim("APPROVED", Map.of());
        assertThat(engine.evaluate(context(approved, Set.of("finance"), "PAYMENT_INITIATED", "", confirmed))
                .effects()).extracting(EffectIntent::id).containsExactly("trigger_payment_system");
        Claim paid = claim("PAYMENT_INITIATED", Map.of());
        assertThat(engine.evaluate(context(paid, Set.of("finance"), "CLOSED", "", confirmed))
                .effects()).extracting(EffectIntent::id).containsExactly("notify_member_payment_reference");
        Claim rejected = claim("REJECTED", Map.of());
        WorkflowEvidence appealElapsed = new WorkflowEvidence(0, null, null, null, now.minus(Duration.ofDays(31)),
                null, false, null, null);
        assertThat(engine.evaluate(context(rejected, Set.of("system"), "CLOSED", "", appealElapsed))
                .effects()).extracting(EffectIntent::id).containsExactly("archive_claim");
    }

    private Claim claim(String state, Map<String, Object> facts) {
        return new Claim(UUID.randomUUID(), "member-1", state, new BigDecimal("10.00"), "USD", 0,
                new ClaimFacts(facts), now, now);
    }

    private WorkflowEvaluationContext context(Claim claim, Set<String> roles, String target, String reason,
                                              WorkflowEvidence evidence) {
        return new WorkflowEvaluationContext(claim, new Actor("actor-1", roles, roles.contains("system")),
                new TransitionCommand(target, claim.version(), reason, new ClaimFacts(Map.of())), evidence, now, Duration.ofDays(30));
    }

    private WorkflowEvidence evidence(int cycles) {
        return new WorkflowEvidence(cycles, null, null, null, null, null, false, null, null);
    }
}
