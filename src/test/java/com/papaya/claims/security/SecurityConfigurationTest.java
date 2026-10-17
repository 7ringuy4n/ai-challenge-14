package com.papaya.claims.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

class SecurityConfigurationTest {
    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void refusesMissingBlankAndPlaceholderCredentials() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        assertThatThrownBy(() -> configuration.users(encoder, "", "system-secret"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLAIMS_DEMO_PASSWORD");
        assertThatThrownBy(() -> configuration.users(encoder, "human-secret", "  "))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLAIMS_SYSTEM_PASSWORD");
        assertThatThrownBy(() -> configuration.users(encoder, "CHANGE_ME_human", "system-secret"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLAIMS_DEMO_PASSWORD");
    }

    @Test
    void configuresDistinctHumanAndProtectedSystemPrincipals() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var users = configuration.users(encoder, "human-secret", "system-secret");
        var clerk = users.loadUserByUsername("clerk");
        var system = users.loadUserByUsername("system");
        assertThat(clerk.getAuthorities()).extracting("authority").contains("ROLE_DOCUMENT_CLERK");
        assertThat(system.getAuthorities()).extracting("authority").containsExactly("ROLE_SYSTEM");
        assertThat(encoder.matches("human-secret", clerk.getPassword())).isTrue();
        assertThat(encoder.matches("system-secret", system.getPassword())).isTrue();
        assertThat(encoder.matches("human-secret", system.getPassword())).isFalse();
    }
}
