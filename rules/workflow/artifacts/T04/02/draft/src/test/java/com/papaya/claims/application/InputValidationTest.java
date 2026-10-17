package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputValidationTest {
    @Test void rejectsOversizedDatabaseAmountAndNullCharacterBeforeInsert() {
        var claims = mock(ClaimRepository.class);
        var service = TransitionServiceTest.service(claims, mock(CommandRepository.class), mock(AuditRepository.class), mock(EffectRepository.class));
        assertThatThrownBy(() -> service.create(new Actor("clerk", "document_clerk"), "member-001", new BigDecimal("1E+20"), "USD", List.of("receipt")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Policy limit");
        assertThatThrownBy(() -> service.create(new Actor("clerk", "document_clerk"), "member" + (char) 0 + "1", BigDecimal.ONE, "USD", List.of("receipt")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("memberId");
        verify(claims, never()).insert(any());
    }
    @Test void emptyFactPatchIsRejected() {
        var claims = mock(ClaimRepository.class);
        var claim = new ClaimSnapshot(java.util.UUID.randomUUID(), "member-001", "UNDER_ASSESSMENT", BigDecimal.TEN, "USD", java.util.Map.of(), 0, 1, null, null);
        when(claims.findForUpdate(claim.id())).thenReturn(java.util.Optional.of(claim));
        var service = TransitionServiceTest.service(claims, mock(CommandRepository.class), mock(AuditRepository.class), mock(EffectRepository.class));
        assertThatThrownBy(() -> service.patchFacts(new Actor("assessor", "assessor"), claim.id(), 1, java.util.Map.of()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("At least one");
    }
}
