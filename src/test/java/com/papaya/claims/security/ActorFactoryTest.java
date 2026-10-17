package com.papaya.claims.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ActorFactoryTest {
    @Test
    void derivesMemberIdentityFromAuthenticatedPrincipalAndRoleAuthority() {
        var authentication = new UsernamePasswordAuthenticationToken("member-42", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        var actor = ActorFactory.from(authentication);
        assertThat(actor.id()).isEqualTo("42");
        assertThat(actor.roles()).containsExactly("member");
        assertThat(actor.system()).isFalse();
    }

    @Test
    void marksSystemOnlyForProtectedSystemPrincipalWithSystemAuthority() {
        var system = ActorFactory.from(new UsernamePasswordAuthenticationToken("system", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))));
        assertThat(system.system()).isTrue();
        assertThatThrownBy(() -> ActorFactory.from(new UsernamePasswordAuthenticationToken("member-1", "ignored")))
                .isInstanceOf(IllegalArgumentException.class);
        var systemNamedUser = ActorFactory.from(new UsernamePasswordAuthenticationToken("ordinary", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))));
        assertThat(systemNamedUser.system()).isFalse();
        assertThat(systemNamedUser.roles()).containsExactly("system");
        var unsupportedRole = ActorFactory.from(new UsernamePasswordAuthenticationToken("staff", "ignored",
                List.of(new SimpleGrantedAuthority("SCOPE_read"), new SimpleGrantedAuthority("ROLE_ASSESSOR"))));
        assertThat(unsupportedRole.roles()).containsExactly("assessor");
        var unauthenticated = new UsernamePasswordAuthenticationToken("staff", "ignored");
        assertThatThrownBy(() -> ActorFactory.from(unauthenticated)).isInstanceOf(IllegalArgumentException.class);
    }
}
