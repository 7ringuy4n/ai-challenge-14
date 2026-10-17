package com.papaya.claims.engine;

import static org.junit.jupiter.api.Assertions.*;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class GuardEvaluatorTest {
    private final GuardEvaluator evaluator = new GuardEvaluator();
    private ClaimSnapshot claim(Map<String,Object> facts) {
        return new ClaimSnapshot(UUID.randomUUID(), "member-001", "X", new BigDecimal("100.00"), "USD", facts, 0, 0, null, null);
    }
    private WorkflowDefinition.Guard guard(String type, String field, String other, Object value) {
        return new WorkflowDefinition.Guard(type, field, other, value, List.of(), "failed");
    }
    @Test void evaluatesBoundaryAmountAndCurrencyEquality() {
        var c = claim(Map.of("assessmentAmount", new BigDecimal("100.00"), "assessmentCurrency", "USD"));
        assertTrue(evaluator.failures(List.of(guard("lessOrEqual", "assessmentAmount", "policyLimit", null), guard("equal", "assessmentCurrency", "currency", null)), c, EngineFixtures.NOW).isEmpty());
    }
    @Test void rejectsAmountAbovePolicyLimit() {
        var c = claim(Map.of("assessmentAmount", 100.01));
        assertFalse(evaluator.failures(List.of(guard("lessOrEqual", "assessmentAmount", "policyLimit", null)), c, EngineFixtures.NOW).isEmpty());
    }
    @Test void checksAfterAndBeforeOrEqualInstantsAtBoundary() {
        var c = claim(Map.of("informationReceivedAt", "2026-10-15T23:59:00Z", "appealDeadline", "2026-10-16T00:00:00Z"));
        assertFalse(evaluator.failures(List.of(guard("after", "informationReceivedAt", null, "2026-10-15T23:59:00Z")), c, EngineFixtures.NOW).isEmpty());
        assertTrue(evaluator.failures(List.of(guard("beforeOrEqual", "appealDeadline", "now", null)), c, EngineFixtures.NOW).isEmpty());
    }
    @Test void malformedFactFailsSafely() {
        assertFalse(evaluator.failures(List.of(guard("lessOrEqual", "assessmentAmount", null, "not money")), claim(Map.of("assessmentAmount", "bad")), EngineFixtures.NOW).isEmpty());
    }
    @Test void allDocumentsRequiresEveryRequiredDocumentValid() {
        var c = claim(Map.of("requiredDocuments", List.of("id", "invoice"), "documents", Map.of("id", true, "invoice", false)));
        assertFalse(evaluator.failures(List.of(guard("allDocumentsValid", "requiredDocuments", null, null)), c, EngineFixtures.NOW).isEmpty());
    }
    @Test void anyAcceptsOneSatisfiedChildGuard() {
        var child = guard("isTrue", "memberAcknowledged", null, null);
        var any = new WorkflowDefinition.Guard("any", null, null, null, List.of(child), "none");
        assertTrue(evaluator.failures(List.of(any), claim(Map.of("memberAcknowledged", true)), EngineFixtures.NOW).isEmpty());
    }
    @Test void missingAndMalformedFactsFailClosed() {
        assertFalse(evaluator.failures(List.of(guard("present", "assessorId", null, null)), claim(Map.of()), EngineFixtures.NOW).isEmpty());
        assertFalse(evaluator.failures(List.of(guard("after", "informationReceivedAt", null, "bad")), claim(Map.of("informationReceivedAt", "bad")), EngineFixtures.NOW).isEmpty());
    }
}
