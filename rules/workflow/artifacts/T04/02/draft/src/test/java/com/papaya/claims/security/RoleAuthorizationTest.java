package com.papaya.claims.security;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class RoleAuthorizationTest {
    @Test void derivesStableActorFromAuthenticatedPrincipalAndRejectsAnonymous() {
        var auth = new UsernamePasswordAuthenticationToken("member-001", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        assertThat(AuthenticatedActors.from(auth).userId()).isEqualTo("member-001");
        assertThat(AuthenticatedActors.from(auth).role()).isEqualTo("member");
        assertThatThrownBy(() -> AuthenticatedActors.from(UsernamePasswordAuthenticationToken.unauthenticated("anonymousUser", "")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
