package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class SystemTransitionTest {
    @Test void systemPrincipalCanUseConfiguredSystemEdgeThroughDedicatedOperation() {
        ClaimRepository claims = mock(ClaimRepository.class);
        AuditRepository audits = mock(AuditRepository.class);
        CommandRepository commands = mock(CommandRepository.class);
        EffectRepository effects = mock(EffectRepository.class);
        ClaimSnapshot rejected = new ClaimSnapshot(UUID.randomUUID(), "member-001", "REJECTED", BigDecimal.TEN, "USD",
                Map.of("memberAcknowledged", true), 0, 4, null, null);
        when(claims.findForUpdate(rejected.id())).thenReturn(Optional.of(rejected));
        when(commands.find(rejected.id(), "system", "close-1")).thenReturn(Optional.empty());
        when(claims.compareAndSet(any(), eq(4L))).thenReturn(true);

        ClaimSnapshot closed = TransitionServiceTest.service(claims, commands, audits, effects)
                .systemTransition(new Actor("system", "system"), rejected.id(),
                        new TransitionCommand("CLOSED", 4, "close-1", "appeal acknowledged"));

        assertThat(closed.state()).isEqualTo("CLOSED");
        verify(audits).append(argThat(entry -> entry.toState().equals("CLOSED") && entry.role().equals("system")));
        verify(commands).insert(eq(rejected.id()), eq("system"), eq("close-1"), any());
    }

    @Test void genericTransitionRejectsSystemAndDedicatedOperationRejectsHumanBeforePersistence() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimService service = TransitionServiceTest.service(claims, mock(CommandRepository.class),
                mock(AuditRepository.class), mock(EffectRepository.class));
        UUID id = UUID.randomUUID();
        TransitionCommand command = new TransitionCommand("CLOSED", 0, "close-2", "close");

        assertThatThrownBy(() -> service.transition(new Actor("system", "system"), id, command))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cannot perform");
        assertThatThrownBy(() -> service.systemTransition(new Actor("finance", "finance"), id, command))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cannot perform");
        verifyNoInteractions(claims);
    }
}
