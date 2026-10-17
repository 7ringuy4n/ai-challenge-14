package com.papaya.claims.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.claim.domain.Actor;
import com.papaya.claims.claim.domain.Claim;
import com.papaya.claims.claim.domain.ClaimFacts;
import com.papaya.claims.claim.domain.TransitionCommand;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuardEvaluatorTest {
    private final GuardEvaluator guards = new GuardEvaluator();
    private final Instant now = Instant.parse("2026-10-17T12:00:00Z");

    @Test
    void validatesDocumentRosterIdentityShapeRequirednessAndPresence() {
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("id-card", doc(true, true, true))), "", WorkflowEvidence.empty(), true);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("id-card", doc(true, true, false))), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("bad id", doc(true, true, true))), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("id-card", Map.of("required", false, "present", true, "valid", true))), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("id-card", Map.of("required", true, "present", true))), "", WorkflowEvidence.empty(), false);
    }

    @Test
    void validatesAssessorReportAndExactAmount() {
        assertGuard("assessor_assigned", Map.of("assessorId", "assessor-2"), "", WorkflowEvidence.empty(), true);
        assertGuard("assessor_assigned", Map.of("assessorId", "  "), "", WorkflowEvidence.empty(), false);
        assertGuard("assessment_report_complete", Map.of("assessmentReport", Map.of("complete", true)), "", WorkflowEvidence.empty(), true);
        assertGuard("assessment_report_complete", Map.of("assessmentReport", Map.of("complete", "true")), "", WorkflowEvidence.empty(), false);
        assertGuard("assessment_report_complete", Map.of("assessmentReport", "complete"), "", WorkflowEvidence.empty(), false);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("10.00")), "", WorkflowEvidence.empty(), true);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("10.01")), "", WorkflowEvidence.empty(), false);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("1.001")), "", WorkflowEvidence.empty(), false);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("-0.01")), "", WorkflowEvidence.empty(), false);
    }

    @Test
    void validatesReasonAndStrictlyNewInformationTimestamp() {
        assertGuard("rejection_reason_present", Map.of(), "reason", WorkflowEvidence.empty(), true);
        assertGuard("rejection_reason_present", Map.of(), "  ", WorkflowEvidence.empty(), false);
        assertGuard("information_description_present", Map.of(), "send the missing invoice", WorkflowEvidence.empty(), true);
        assertGuard("information_description_present", Map.of(), "", WorkflowEvidence.empty(), false);
        var requested = Instant.parse("2026-10-17T10:00:00Z");
        assertGuard("new_information_received", Map.of(), "", new WorkflowEvidence(1, requested, requested.plusNanos(1_000), null, null, null, false, null, null), true);
        assertGuard("new_information_received", Map.of(), "", new WorkflowEvidence(1, requested, requested, null, null, null, false, null, null), false);
        assertGuard("new_information_received", Map.of(), "", new WorkflowEvidence(1, requested, requested.minusSeconds(1), null, null, null, false, null, null), false);
    }

    @Test
    void requiresPersistedPaymentAndConfirmedReference() {
        assertGuard("payment_request_exists", Map.of(), "", WorkflowEvidence.empty(), false);
        assertGuard("payment_request_exists", Map.of(), "", new WorkflowEvidence(0, null, null, null, null, null, true, null, null), true);
        assertGuard("payment_confirmed", Map.of(), "", new WorkflowEvidence(0, null, null, null, null, null, true, "REF-1", now), true);
        assertGuard("payment_confirmed", Map.of(), "", new WorkflowEvidence(0, null, null, null, null, null, false, "REF-1", now), false);
        assertGuard("payment_confirmed", Map.of(), "", new WorkflowEvidence(0, null, null, null, null, null, true, " ", now), false);
    }

    @Test
    void rejectsUnknownStrategiesMalformedDocumentsAndOutOfRangeAmounts() {
        assertThat(guards.passes("unknown", context(Map.of(), WorkflowEvidence.empty()))).isFalse();
        assertGuard("all_required_documents_present", Map.of("documents", "not-an-object"), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("invoice", "not-an-object")), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("invoice", doc(false, false, true))), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("invoice", doc(true, true, false))), "", WorkflowEvidence.empty(), false);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", "10.00"), "", WorkflowEvidence.empty(), false);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("10000000000000000.00")), "", WorkflowEvidence.empty(), false);
    }

    @Test
    void paymentConfirmationAndInformationTimestampRequireCompleteTrustedEvidence() {
        assertGuard("payment_confirmed", Map.of(), "", new WorkflowEvidence(0, null, null, null, null, null, true, "REF-2", null), false);
        assertGuard("payment_confirmed", Map.of(), "", new WorkflowEvidence(0, null, null, null, null, null, true, null, now), false);
        assertGuard("new_information_received", Map.of(), "", new WorkflowEvidence(0, null, now, null, null, null, false, null, null), false);
    }

    private WorkflowEvaluationContext context(Map<String, Object> facts, WorkflowEvidence evidence) {
        Claim claim = new Claim(UUID.randomUUID(), "member-1", "UNDER_ASSESSMENT", new BigDecimal("10.00"), "USD",
                0, new ClaimFacts(facts), now, now);
        return new WorkflowEvaluationContext(claim, new Actor("actor-1", Set.of("assessor"), false),
                new TransitionCommand("APPROVED", 0, "", new ClaimFacts(Map.of())), evidence, now, Duration.ofDays(30));
    }

    @Test
    void enforcesAppealExpiryOrAcknowledgementAfterRejectionAtMicrosecondPrecision() {
        Instant rejected = Instant.parse("2026-09-17T12:00:00Z");
        assertGuard("appeal_period_expired_or_member_acknowledged", Map.of(), "",
                new WorkflowEvidence(0, null, null, null, rejected, null, false, null, null), true);
        Instant oneMicrosecondBefore = Instant.parse("2026-10-17T11:59:59.999999Z");
        assertGuardAt("appeal_period_expired_or_member_acknowledged", Map.of(), "",
                new WorkflowEvidence(0, null, null, null, rejected, null, false, null, null), oneMicrosecondBefore, false);
        assertGuard("appeal_period_expired_or_member_acknowledged", Map.of(), "",
                new WorkflowEvidence(0, null, null, null, rejected, rejected.plusSeconds(1), false, null, null), true);
        assertGuardAt("appeal_period_expired_or_member_acknowledged", Map.of(), "",
                new WorkflowEvidence(0, null, null, null, rejected, rejected.minusNanos(1_000), false, null, null),
                Instant.parse("2026-10-01T12:00:00Z"), false);
    }

    @Test
    void rejectsDocumentRostersWithoutRequiredDocumentsAndIdentifiersOutsideTheContract() {
        assertGuard("all_required_documents_present", Map.of("documents", Map.of()), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("invoice/1", doc(true, true, true))), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("invoice", doc(false, true, true))), "", WorkflowEvidence.empty(), false);
        assertGuard("all_required_documents_present", Map.of("documents", Map.of("invoice", doc(true, true, true), "receipt", doc(true, true, true))), "", WorkflowEvidence.empty(), true);
    }

    @Test
    void amountGuardHandlesExactBoundariesAndRejectsInvalidScaleAndPrecision() {
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("0.00")), "", WorkflowEvidence.empty(), true);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("10.000")), "", WorkflowEvidence.empty(), false);
        assertGuard("amount_within_policy_limit", Map.of("requestedAmount", new BigDecimal("10000000000000000")), "", WorkflowEvidence.empty(), false);
    }

    @Test
    void workflowEvidenceNormalizesEachTrustedTimestampAndRejectsUnsupportedCounters() {
        Instant precise = Instant.parse("2026-10-17T12:00:00.123456789Z");
        WorkflowEvidence evidence = new WorkflowEvidence(0, precise, precise, precise, precise, precise,
                false, null, precise);
        Instant expected = Instant.parse("2026-10-17T12:00:00.123456Z");
        assertThat(evidence.lastInformationRequestedAt()).isEqualTo(expected);
        assertThat(evidence.lastInformationReceivedAt()).isEqualTo(expected);
        assertThat(evidence.assessmentStartedAt()).isEqualTo(expected);
        assertThat(evidence.rejectedAt()).isEqualTo(expected);
        assertThat(evidence.memberAcknowledgedRejectionAt()).isEqualTo(expected);
        assertThat(evidence.paymentConfirmedAt()).isEqualTo(expected);
        assertThat(evidence.counterValue("informationRequests")).isZero();
        assertThatThrownBy(() -> evidence.counterValue("unsupported")).isInstanceOf(IllegalArgumentException.class);
    }

    private Map<String, Object> doc(boolean required, boolean present, boolean valid) {
        return Map.of("required", required, "present", present, "valid", valid);
    }

    private void assertGuard(String guard, Map<String, Object> facts, String reason, WorkflowEvidence evidence, boolean expected) {
        assertGuardAt(guard, facts, reason, evidence, now, expected);
    }

    private void assertGuardAt(String guard, Map<String, Object> facts, String reason, WorkflowEvidence evidence,
                               Instant at, boolean expected) {
        Claim claim = new Claim(UUID.randomUUID(), "member-1", "UNDER_ASSESSMENT", new BigDecimal("10.00"), "USD",
                0, new ClaimFacts(facts), at, at);
        var context = new WorkflowEvaluationContext(claim, new Actor("actor-1", Set.of("assessor"), false),
                new TransitionCommand("APPROVED", 0, reason, new ClaimFacts(Map.of())), evidence, at, Duration.ofDays(30));
        assertThat(guards.passes(guard, context)).as(guard).isEqualTo(expected);
    }
}
