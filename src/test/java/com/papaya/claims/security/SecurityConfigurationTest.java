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
        assertThatThrownBy(() -> configuration.users(encoder, "", "system-secret", "member-demo"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLAIMS_DEMO_PASSWORD");
        assertThatThrownBy(() -> configuration.users(encoder, "human-secret", "  ", "member-demo"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLAIMS_SYSTEM_PASSWORD");
        assertThatThrownBy(() -> configuration.users(encoder, "CHANGE_ME_human", "system-secret", "member-demo"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLAIMS_DEMO_PASSWORD");
    }

    @Test
    void configuresDistinctHumanAndProtectedSystemPrincipals() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var users = configuration.users(encoder, "human-secret", "system-secret", "member-demo");
        var clerk = users.loadUserByUsername("clerk");
        var system = users.loadUserByUsername("system");
        assertThat(clerk.getAuthorities()).extracting("authority").contains("ROLE_DOCUMENT_CLERK");
        assertThat(system.getAuthorities()).extracting("authority").containsExactly("ROLE_SYSTEM");
        assertThat(encoder.matches("human-secret", clerk.getPassword())).isTrue();
        assertThat(encoder.matches("system-secret", system.getPassword())).isTrue();
        assertThat(encoder.matches("human-secret", system.getPassword())).isFalse();
    }

    @Test
    void configuresDistinctMemberPrincipalsWithMemberRoleAndHumanCredential() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var users = configuration.users(encoder, "human-secret", "system-secret", "member-alice, member-bob");

        var alice = users.loadUserByUsername("member-alice");
        var bob = users.loadUserByUsername("member-bob");
        assertThat(alice.getAuthorities()).extracting("authority").containsExactly("ROLE_MEMBER");
        assertThat(bob.getAuthorities()).extracting("authority").containsExactly("ROLE_MEMBER");
        assertThat(encoder.matches("human-secret", alice.getPassword())).isTrue();
        assertThat(encoder.matches("human-secret", bob.getPassword())).isTrue();
        assertThat(users.userExists("member-demo")).isFalse();
    }

    @Test
    void rejectsMalformedDuplicateOrNonMemberIdentities() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        assertThatThrownBy(() -> configuration.users(encoder, "human-secret", "system-secret", ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CLAIMS_MEMBER_USERNAMES");
        assertThatThrownBy(() -> configuration.users(encoder, "human-secret", "system-secret", "member-alice,member-alice"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unique");
        assertThatThrownBy(() -> configuration.users(encoder, "human-secret", "system-secret", "system"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("member-");
    }

    @Test
    void acceptsMaximumMemberNameThatFitsDatabaseIdentityAndRejectsLongerName() {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String maximum = "member-" + "a".repeat(120);
        String tooLong = "member-" + "a".repeat(121);

        var users = configuration.users(encoder, "human-secret", "system-secret", maximum);

        assertThat(users.userExists(maximum)).isTrue();
        assertThatThrownBy(() -> configuration.users(encoder, "human-secret", "system-secret", tooLong))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("127 characters");
    }
}
