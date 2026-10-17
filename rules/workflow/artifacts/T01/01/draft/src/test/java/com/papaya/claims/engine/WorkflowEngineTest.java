package com.papaya.claims.engine;

import static org.junit.jupiter.api.Assertions.*;
import static com.papaya.claims.engine.EngineFixtures.*;
import com.papaya.claims.contract.Model.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorkflowEngineTest {
    @Test void submittedToDocumentsVerifiedRequiresValidDocuments() {
        var d = evaluate("SUBMITTED", "DOCUMENTS_VERIFIED", "document_clerk", validDocs(), 0);
        assertEquals(List.of("notify_assessors"), d.effects());
    }
    @Test void assignmentStartsAssessmentTimer() {
        var d = evaluate("DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT", "team_lead", Map.of("assessorId", "assessor-7"), 0);
        assertEquals(NOW, d.assessmentStartedAt());
    }
    @Test void approvalChecksReportLimitCurrencyAndDocuments() {
        var f = validDocs(); f.put("reportComplete", true); f.put("assessmentAmount", 100); f.put("assessmentCurrency", "USD");
        assertEquals(List.of("notify_approval", "create_payment_request"), evaluate("UNDER_ASSESSMENT", "APPROVED", "assessor", f, 0).effects());
    }
    @Test void rejectionRequiresReasonAndCompletedReport() {
        assertEquals("REJECTED", evaluate("UNDER_ASSESSMENT", "REJECTED", "assessor", Map.of("reportComplete", true, "rejectionReason", "Not covered"), 0).toState());
    }
    @Test void informationRequestClearsStaleAssessment() {
        var d = evaluate("UNDER_ASSESSMENT", "PENDING_INFO", "assessor", Map.of("missingInfoDescription", "Need receipt"), 0);
        assertEquals(1, d.informationRequests());
        assertEquals(List.of("reportComplete", "assessmentAmount", "assessmentCurrency"), d.clearFacts());
        assertEquals(NOW, d.lastInformationRequestedAt());
    }
    @Test void returningWithNewInformationResetsTimer() {
        var facts = validDocs(); facts.put("informationReceivedAt", NOW.toString());
        assertNull(evaluate("PENDING_INFO", "DOCUMENTS_VERIFIED", "document_clerk", facts, 1).assessmentStartedAt());
    }
    @Test void financeCanStartPaymentOnlyWhenRequestExists() {
        assertEquals("PAYMENT_INITIATED", evaluate("APPROVED", "PAYMENT_INITIATED", "finance", Map.of("paymentRequestCreated", true), 0).toState());
    }
    @Test void financeClosesConfirmedPayment() {
        assertEquals("CLOSED", evaluate("PAYMENT_INITIATED", "CLOSED", "finance", Map.of("paymentConfirmed", true, "paymentReference", "PAY-1"), 0).toState());
    }
    @Test void systemClosesAcknowledgedRejection() {
        assertEquals("CLOSED", evaluate("REJECTED", "CLOSED", "system", Map.of("memberAcknowledged", true), 0).toState());
    }
    @Test void fourthInformationRequestHasExactEscalationMessage() {
        var error = assertThrows(com.papaya.claims.contract.BusinessException.class, () -> evaluate("UNDER_ASSESSMENT", "PENDING_INFO", "assessor", Map.of("missingInfoDescription", "More"), 3));
        assertEquals(422, error.status()); assertEquals(MAX_REQUESTS_MESSAGE, error.getMessage());
    }
    @Test void thirdInformationRequestIsAccepted() {
        var d = evaluate("UNDER_ASSESSMENT", "PENDING_INFO", "assessor", Map.of("missingInfoDescription", "More"), 2);
        assertEquals(3, d.informationRequests());
    }
    @Test void unauthorizedEdgeIsVisibleButNotExecutable() {
        var claim = claim("SUBMITTED", validDocs(), 0);
        var option = ENGINE.options(claim, new Actor("lead", "team_lead"), NOW).getFirst();
        assertFalse(option.allowed()); assertTrue(option.blockers().getFirst().contains("document_clerk"));
        expect(403, "FORBIDDEN_ROLE", () -> ENGINE.evaluate(claim, new Actor("lead", "team_lead"), new TransitionCommand("DOCUMENTS_VERIFIED", 1, "k", ""), NOW));
    }
    @Test void unknownEdgeReturnsSpecificConflict() {
        expect(409, "INVALID_TRANSITION", () -> evaluate("SUBMITTED", "APPROVED", "assessor", Map.of(), 0));
    }
    @Test void optionsReportGuardBlockers() {
        var option = ENGINE.options(claim("UNDER_ASSESSMENT", Map.of(), 0), new Actor("a", "assessor"), NOW).getFirst();
        assertFalse(option.allowed()); assertTrue(option.blockers().stream().anyMatch(s -> s.contains("report")));
    }
    @Test void definitionAndReturnedListsAreImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> ENGINE.options(claim("SUBMITTED", validDocs(), 0), new Actor("c", "document_clerk"), NOW).clear());
    }
}
