package com.papaya.claims.security;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityBoundaryTest {
    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test void refusesMissingBlankOrSharedHumanAndSystemCredentials() {
        PasswordEncoder encoder = configuration.passwordEncoder();
        assertThatThrownBy(() -> configuration.userDetailsService(encoder, null, "system-secret"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Separate nonempty");
        assertThatThrownBy(() -> configuration.userDetailsService(encoder, "   ", "system-secret"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.userDetailsService(encoder, "same-secret", "same-secret"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.userDetailsService(encoder, "human-secret", " "))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.userDetailsService(encoder, "human-secret", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void configuresDistinctHumanAndProtectedSystemAccountsWithExpectedAuthorities() {
        PasswordEncoder encoder = configuration.passwordEncoder();
        UserDetailsService users = configuration.userDetailsService(encoder, "human-secret", "system-secret");
        assertThat(users.loadUserByUsername("clerk").getAuthorities()).extracting(Object::toString).containsExactly("ROLE_DOCUMENT_CLERK");
        assertThat(users.loadUserByUsername("member-001").getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MEMBER");
        assertThat(users.loadUserByUsername("system").getAuthorities()).extracting(Object::toString).containsExactly("ROLE_SYSTEM");
        assertThat(encoder.matches("human-secret", users.loadUserByUsername("clerk").getPassword())).isTrue();
        assertThat(encoder.matches("system-secret", users.loadUserByUsername("system").getPassword())).isTrue();
        assertThat(encoder.matches("human-secret", users.loadUserByUsername("system").getPassword())).isFalse();
    }

    @Test void authenticatedActorConversionRejectsAbsentAnonymousAndRolelessPrincipals() {
        assertThatThrownBy(() -> AuthenticatedActors.from(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuthenticatedActors.from(UsernamePasswordAuthenticationToken.unauthenticated("anonymousUser", "")))
                .isInstanceOf(IllegalArgumentException.class);
        var anonymousNamed = new UsernamePasswordAuthenticationToken("anonymousUser", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        assertThatThrownBy(() -> AuthenticatedActors.from(anonymousNamed)).isInstanceOf(IllegalArgumentException.class);
        var roleless = new UsernamePasswordAuthenticationToken("service-user", "n/a", List.of(new SimpleGrantedAuthority("SCOPE_claims")));
        assertThatThrownBy(() -> AuthenticatedActors.from(roleless)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no workflow role");
    }

    @Test void authenticatedActorUsesFirstWorkflowRoleAndNormalizesItsCase() {
        var authentication = new UsernamePasswordAuthenticationToken("lead-user", "n/a", List.of(
                new SimpleGrantedAuthority("SCOPE_claims"), new SimpleGrantedAuthority("ROLE_TEAM_LEAD"),
                new SimpleGrantedAuthority("ROLE_ASSESSOR")));
        assertThat(AuthenticatedActors.from(authentication).userId()).isEqualTo("lead-user");
        assertThat(AuthenticatedActors.from(authentication).role()).isEqualTo("team_lead");
    }
}
