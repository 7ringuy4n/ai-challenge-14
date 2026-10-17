package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class IdempotencyTest {
    @Test void changedPayloadForPreviouslyRecordedKeyIsConflict() {
        CommandRepository commands = mock(CommandRepository.class);
        ClaimSnapshot claim = new ClaimSnapshot(UUID.randomUUID(), "member-001", "APPROVED", BigDecimal.TEN, "USD", Map.of(), 0, 5, null, null);
        when(commands.find(claim.id(), "finance", "same-key")).thenReturn(Optional.of(new CommandReceipt(hash("CLOSED", 3, "same payload", "finance"), claim)));
        ClaimRepository claims = mock(ClaimRepository.class); when(claims.findForUpdate(claim.id())).thenReturn(Optional.of(claim));
        ClaimService service = TransitionServiceTest.service(claims, commands, mock(AuditRepository.class), mock(EffectRepository.class));
        assertThatThrownBy(() -> service.transition(new Actor("finance", "finance"), claim.id(), new TransitionCommand("CLOSED", 3, "same-key", "new payload")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("different command");
    }
    @Test void changedRoleForSameUserCannotReplayReceipt() {
        CommandRepository commands = mock(CommandRepository.class);
        ClaimSnapshot claim = new ClaimSnapshot(UUID.randomUUID(), "member-001", "APPROVED", BigDecimal.TEN, "USD", Map.of(), 0, 5, null, null);
        when(commands.find(claim.id(), "finance", "same-key")).thenReturn(Optional.of(new CommandReceipt(hash("CLOSED", 3, "note", "finance"), claim)));
        ClaimRepository claims = mock(ClaimRepository.class); when(claims.findForUpdate(claim.id())).thenReturn(Optional.of(claim));
        ClaimService service = TransitionServiceTest.service(claims, commands, mock(AuditRepository.class), mock(EffectRepository.class));
        assertThatThrownBy(() -> service.transition(new Actor("finance", "assessor"), claim.id(), new TransitionCommand("CLOSED", 3, "same-key", "note")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("different command");
    }

    private static String hash(String target, long version, String notes, String role) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest((target + "\u0000" + version + "\u0000" + notes + "\u0000" + role).getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
