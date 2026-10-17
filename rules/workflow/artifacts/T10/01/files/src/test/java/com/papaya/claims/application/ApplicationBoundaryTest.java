package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ApplicationBoundaryTest {
    private static final Actor CLERK = new Actor("clerk", "document_clerk");

    @Test void createRejectsBadIdentityMoneyCurrencyAndDocumentCollectionsBeforePersistence() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimService service = service(claims);
        List<ArgumentsForCreate> invalid = List.of(
                new ArgumentsForCreate(null, BigDecimal.ONE, "USD", List.of("receipt")),
                new ArgumentsForCreate(" ", BigDecimal.ONE, "USD", List.of("receipt")),
                new ArgumentsForCreate("member" + (char) 0 + "001", BigDecimal.ONE, "USD", List.of("receipt")),
                new ArgumentsForCreate("m".repeat(121), BigDecimal.ONE, "USD", List.of("receipt")),
                new ArgumentsForCreate("member-001", null, "USD", List.of("receipt")),
                new ArgumentsForCreate("member-001", new BigDecimal("-0.01"), "USD", List.of("receipt")),
                new ArgumentsForCreate("member-001", new BigDecimal("1.001"), "USD", List.of("receipt")),
                new ArgumentsForCreate("member-001", new BigDecimal("100000000000000000"), "USD", List.of("receipt")),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "usd", List.of("receipt")),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "ZZZ", List.of("receipt")),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "USD", null),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "USD", List.of()),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "USD", Collections.nCopies(31, "receipt")),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "USD", List.of("receipt", "receipt")),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "USD", List.of(" ")),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "USD", List.of("doc".repeat(27))),
                new ArgumentsForCreate("member-001", BigDecimal.ONE, "USD", List.of("receipt" + (char) 0)));
        for (ArgumentsForCreate args : invalid) {
            assertThatThrownBy(() -> service.create(CLERK, args.memberId(), args.limit(), args.currency(), args.documents()))
                    .as("invalid create input %s", args)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).status()).isEqualTo(400));
        }
        verify(claims, never()).insert(any());
    }

    @Test void memberMayCreateOnlyForSelfAndOtherRolesCannotCreate() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimService service = service(claims);
        assertBusiness(() -> service.create(new Actor("member-001", "member"), "member-002", BigDecimal.ONE, "USD", List.of("receipt")), "FORBIDDEN_ROLE", 403);
        assertBusiness(() -> service.create(new Actor("system", "system"), "member-001", BigDecimal.ONE, "USD", List.of("receipt")), "FORBIDDEN_ROLE", 403);
        verify(claims, never()).insert(any());
    }

    @Test void getAndListFailClosedForMissingClaimsAndUnrecognizedRolesAndFilterMemberOwnership() {
        ClaimRepository claims = mock(ClaimRepository.class);
        UUID missing = UUID.randomUUID();
        ClaimSnapshot first = claim("member-001");
        ClaimSnapshot second = claim("member-002");
        when(claims.find(missing)).thenReturn(Optional.empty());
        when(claims.find(first.id())).thenReturn(Optional.of(first));
        when(claims.list()).thenReturn(List.of(first, second));
        ClaimService service = service(claims);

        assertBusiness(() -> service.get(new Actor("clerk", "document_clerk"), missing), "NOT_FOUND", 404);
        assertBusiness(() -> service.get(new Actor("member-002", "member"), first.id()), "FORBIDDEN_ROLE", 403);
        assertBusiness(() -> service.list(new Actor("auditor", "auditor")), "FORBIDDEN_ROLE", 403);
        assertThat(service.list(new Actor("member-001", "member"))).extracting(ClaimSnapshot::memberId).containsExactly("member-001");
        assertThat(service.list(new Actor("clerk", "document_clerk"))).hasSize(2);
    }

    @Test void factMutationAllowsEveryRoleOnlyInItsPhaseAndUsesServerStampedInformationTime() {
        assertPatchSucceeds("document_clerk", "clerk", "SUBMITTED", Map.of("documents", Map.of("receipt", true)), false);
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimSnapshot pending = claim("member-001", "PENDING_INFO", Map.of("requiredDocuments", List.of("receipt")));
        when(claims.findForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(claims.compareAndSet(any(), eq(pending.version()))).thenReturn(true);
        ClaimSnapshot updated = service(claims).patchFacts(new Actor("clerk", "document_clerk"), pending.id(), pending.version(), Map.of("newInformation", "invoice attached"));
        assertThat(updated.facts()).containsEntry("newInformation", "invoice attached").containsKey("informationReceivedAt");
        assertThat(updated.facts().get("informationReceivedAt")).isInstanceOf(String.class);

        assertPatchSucceeds("team_lead", "lead", "DOCUMENTS_VERIFIED", Map.of("assessorId", "assessor-1"), false);
        assertPatchSucceeds("assessor", "assessor", "UNDER_ASSESSMENT", Map.of("reportComplete", true, "assessmentAmount", new BigDecimal("12.30"), "assessmentCurrency", "USD", "rejectionReason", "not covered", "missingInfoDescription", "send receipt"), false);
        assertPatchSucceeds("finance", "finance", "PAYMENT_INITIATED", Map.of("paymentConfirmed", true, "paymentReference", "pay-17"), true);
        assertPatchSucceeds("member", "member-001", "REJECTED", Map.of("memberAcknowledged", true), false);
    }

    @Test void financeRequiresExistingPaymentRequestAndNonblankReference() {
        ClaimRepository claims = mock(ClaimRepository.class);
        EffectRepository effects = mock(EffectRepository.class);
        ClaimSnapshot payment = claim("member-001", "PAYMENT_INITIATED", Map.of("requiredDocuments", List.of("receipt")));
        when(claims.findForUpdate(payment.id())).thenReturn(Optional.of(payment));
        ClaimService service = service(claims, effects);

        when(effects.paymentRequestExists(payment.id())).thenReturn(false);
        assertBusiness(() -> service.patchFacts(new Actor("finance", "finance"), payment.id(), payment.version(), Map.of("paymentConfirmed", true, "paymentReference", "ref-1")), "CONFLICT", 409);
        verify(claims, never()).compareAndSet(any(), anyLong());

        when(effects.paymentRequestExists(payment.id())).thenReturn(true);
        assertBusiness(() -> service.patchFacts(new Actor("finance", "finance"), payment.id(), payment.version(), Map.of("paymentConfirmed", true)), "INVALID_INPUT", 400);
        assertBusiness(() -> service.patchFacts(new Actor("finance", "finance"), payment.id(), payment.version(), Map.of("paymentReference", " ")), "INVALID_INPUT", 400);
        verify(claims, never()).compareAndSet(any(), anyLong());
    }

    @Test void factsRejectBadNamesTypesRangesAndDocumentShapesWithoutChangingVersion() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimSnapshot assessment = claim("member-001", "UNDER_ASSESSMENT", Map.of("requiredDocuments", List.of("receipt")));
        when(claims.findForUpdate(assessment.id())).thenReturn(Optional.of(assessment));
        ClaimService service = service(claims);
        Actor assessor = new Actor("assessor", "assessor");
        List<Map<String, Object>> badFacts = List.of(
                Map.of("reportComplete", "true"), Map.of("assessmentAmount", "not-money"),
                Map.of("assessmentAmount", new BigDecimal("-0.01")), Map.of("assessmentAmount", new BigDecimal("1.001")),
                Map.of("assessmentAmount", new BigDecimal("100000000000000000")), Map.of("assessmentCurrency", "usd"),
                Map.of("assessmentCurrency", 12), Map.of("rejectionReason", " "),
                Map.of("rejectionReason", "x".repeat(2001)), Map.of("missingInfoDescription", "contains" + (char) 0 + "nul"),
                Map.of("documents", "receipt"), Map.of("documents", Map.of("other", true)),
                Map.of("documents", Map.of("receipt", "yes")), Map.of("documents", Map.of(1, true)));
        for (Map<String, Object> updates : badFacts) {
            assertThatThrownBy(() -> service.patchFacts(assessor, assessment.id(), assessment.version(), updates))
                    .as("invalid fact patch %s", updates).isInstanceOf(BusinessException.class);
        }
        Map<String, Object> nulKey = new LinkedHashMap<>(); nulKey.put("bad" + (char) 0, true);
        assertThatThrownBy(() -> service.patchFacts(assessor, assessment.id(), assessment.version(), nulKey))
                .isInstanceOf(BusinessException.class).hasMessageContaining("null characters");
        verify(claims, never()).compareAndSet(any(), anyLong());
    }

    @Test void rejectsUpdatesFromWrongRolePhaseUnknownFactStaleVersionAndNullCommandText() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimSnapshot submitted = claim("member-001", "SUBMITTED", Map.of());
        when(claims.findForUpdate(submitted.id())).thenReturn(Optional.of(submitted));
        ClaimService service = service(claims);
        assertBusiness(() -> service.patchFacts(new Actor("lead", "team_lead"), submitted.id(), submitted.version(), Map.of("assessorId", "a-1")), "CONFLICT", 409);
        assertBusiness(() -> service.patchFacts(new Actor("clerk", "document_clerk"), submitted.id(), submitted.version(), Map.of("serverFact", true)), "INVALID_INPUT", 400);
        assertBusiness(() -> service.patchFacts(new Actor("clerk", "document_clerk"), submitted.id(), submitted.version() + 1, Map.of("documents", Map.of("receipt", true))), "CONFLICT", 409);
        assertBusiness(() -> service.transition(new Actor("assessor", "assessor"), submitted.id(), new TransitionCommand("VERIFIED", 0, "bad" + (char) 0, "note")), "INVALID_INPUT", 400);
        assertBusiness(() -> service.transition(new Actor("assessor", "assessor" + (char) 0), submitted.id(), new TransitionCommand("VERIFIED", 0, "key", "note")), "INVALID_INPUT", 400);
        verify(claims, never()).compareAndSet(any(), anyLong());
    }

    @Test void mockEffectFactoryHandlesPaymentMarkerUnknownIdsAndSerializationFailures() {
        MockEffectFactory factory = new MockEffectFactory(JsonMapper.builder().findAndAddModules().build());
        ClaimSnapshot claim = claim("member-001", "SUBMITTED", Map.of());
        assertThat(factory.supports("create_payment_request")).isTrue();
        assertThat(factory.supports("not-configured")).isFalse();
        assertThatThrownBy(() -> factory.strategy("not-configured"))
                .isInstanceOf(BusinessException.class).satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("UNKNOWN_EFFECT"));
        ObjectMapper brokenJson = mock(ObjectMapper.class);
        try { when(brokenJson.writeValueAsString(any())).thenThrow(new JsonProcessingException("synthetic failure") {}); }
        catch (JsonProcessingException impossible) { throw new AssertionError(impossible); }
        MockEffectFactory brokenFactory = new MockEffectFactory(brokenJson);
        assertThatThrownBy(() -> brokenFactory.create("notify_assessors", claim, new Actor("clerk", "document_clerk"), ""))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Unable to encode effect payload");
    }

    private static void assertPatchSucceeds(String role, String user, String state, Map<String, Object> updates, boolean paymentExists) {
        ClaimRepository claims = mock(ClaimRepository.class);
        EffectRepository effects = mock(EffectRepository.class);
        ClaimSnapshot claim = claim("member-001", state, Map.of("requiredDocuments", List.of("receipt")));
        when(claims.findForUpdate(claim.id())).thenReturn(Optional.of(claim));
        when(claims.compareAndSet(any(), eq(claim.version()))).thenReturn(true);
        when(effects.paymentRequestExists(claim.id())).thenReturn(paymentExists);
        ClaimSnapshot updated = service(claims, effects).patchFacts(new Actor(user, role), claim.id(), claim.version(), updates);
        assertThat(updated.version()).isEqualTo(claim.version() + 1);
        assertThat(updated.facts()).containsAllEntriesOf(updates);
        verify(claims).compareAndSet(any(), eq(claim.version()));
    }

    private static ClaimService service(ClaimRepository claims) { return service(claims, mock(EffectRepository.class)); }
    private static ClaimService service(ClaimRepository claims, EffectRepository effects) {
        return TransitionServiceTest.service(claims, mock(CommandRepository.class), mock(AuditRepository.class), effects);
    }
    private static ClaimSnapshot claim(String member) { return claim(member, "SUBMITTED", Map.of()); }
    private static ClaimSnapshot claim(String member, String state, Map<String, Object> facts) {
        return new ClaimSnapshot(UUID.randomUUID(), member, state, new BigDecimal("100.00"), "USD", facts, 0, 0, null, null);
    }
    private static void assertBusiness(Runnable action, String code, int status) {
        assertThatThrownBy(action).isInstanceOf(BusinessException.class).satisfies(error -> {
            BusinessException business = (BusinessException) error;
            assertThat(business.code()).isEqualTo(code);
            assertThat(business.status()).isEqualTo(status);
        });
    }
    private record ArgumentsForCreate(String memberId, BigDecimal limit, String currency, List<String> documents) {}
}
