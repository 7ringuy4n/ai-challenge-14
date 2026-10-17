package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class FactAuthorizationTest {
    @Test void rejectsClientOwnedServerFactsAndAssessmentEditsAfterApproval() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimSnapshot claim = new ClaimSnapshot(UUID.randomUUID(), "member-001", "APPROVED", BigDecimal.TEN, "USD", Map.of(), 0, 1, null, null);
        when(claims.findForUpdate(claim.id())).thenReturn(Optional.of(claim));
        ClaimService service = TransitionServiceTest.service(claims, mock(CommandRepository.class), mock(AuditRepository.class), mock(EffectRepository.class));
        assertThatThrownBy(() -> service.patchFacts(new Actor("assessor", "assessor"), claim.id(), 1, Map.of("assessmentAmount", BigDecimal.ONE)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("current claim state");
        assertThatThrownBy(() -> service.patchFacts(new Actor("assessor", "assessor"), claim.id(), 1, Map.of("paymentRequestCreated", true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("not writable");
        verify(claims, never()).compareAndSet(any(), anyLong());
    }
    @Test void memberCanAcknowledgeOnlyOwnRejectedClaim() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimSnapshot rejected = new ClaimSnapshot(UUID.randomUUID(), "member-001", "REJECTED", BigDecimal.TEN, "USD", Map.of(), 0, 2, null, null);
        when(claims.findForUpdate(rejected.id())).thenReturn(Optional.of(rejected));
        ClaimService service = TransitionServiceTest.service(claims, mock(CommandRepository.class), mock(AuditRepository.class), mock(EffectRepository.class));
        when(claims.compareAndSet(any(), eq(2L))).thenReturn(true);
        assertThat(service.patchFacts(new Actor("member-001", "member"), rejected.id(), 2, Map.of("memberAcknowledged", true)).facts()).containsEntry("memberAcknowledged", true);
        assertThatThrownBy(() -> service.patchFacts(new Actor("member-002", "member"), rejected.id(), 2, Map.of("memberAcknowledged", true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cannot perform");
    }
    @Test void configuredStateWithoutOutgoingEdgesIsTerminalForFactMutation() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimSnapshot closed = new ClaimSnapshot(UUID.randomUUID(), "member-001", "CLOSED", BigDecimal.TEN, "USD", Map.of(), 0, 7, null, null);
        when(claims.findForUpdate(closed.id())).thenReturn(Optional.of(closed));
        ClaimService service = TransitionServiceTest.service(claims, mock(CommandRepository.class), mock(AuditRepository.class), mock(EffectRepository.class));
        assertThatThrownBy(() -> service.patchFacts(new Actor("member-001", "member"), closed.id(), 7, Map.of("memberAcknowledged", true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Terminal claims");
        verify(claims, never()).compareAndSet(any(), anyLong());
    }

    @Test void memberOptionsExposeConfiguredEdgesAsBlocked() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimSnapshot own = new ClaimSnapshot(UUID.randomUUID(), "member-001", "UNDER_ASSESSMENT", BigDecimal.TEN, "USD", Map.of(), 0, 1, null, null);
        when(claims.find(own.id())).thenReturn(Optional.of(own));
        ClaimService service = TransitionServiceTest.service(claims, mock(CommandRepository.class), mock(AuditRepository.class), mock(EffectRepository.class));
        var options = service.options(new Actor("member-001", "member"), own.id());
        assertThat(options).isNotEmpty().allSatisfy(option -> assertThat(option.allowed()).isFalse());
    }

}
