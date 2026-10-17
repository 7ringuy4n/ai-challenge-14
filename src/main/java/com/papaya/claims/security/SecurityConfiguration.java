package com.papaya.claims.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private static final int MAX_MEMBER_USERNAME_LENGTH = 127;

    @Bean
    PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

    @Bean
    InMemoryUserDetailsManager users(PasswordEncoder encoder,
            @Value("${claims.security.demo-password:}") String humanPassword,
            @Value("${claims.security.system-password:}") String systemPassword,
            @Value("${claims.security.member-usernames:member-demo}") String memberUsernames) {
        requireConfiguredSecret(humanPassword, "CLAIMS_DEMO_PASSWORD");
        requireConfiguredSecret(systemPassword, "CLAIMS_SYSTEM_PASSWORD");
        List<UserDetails> configuredUsers = new ArrayList<>(List.of(
                User.withUsername("clerk").password(encoder.encode(humanPassword)).roles("DOCUMENT_CLERK").build(),
                User.withUsername("lead").password(encoder.encode(humanPassword)).roles("TEAM_LEAD").build(),
                User.withUsername("assessor").password(encoder.encode(humanPassword)).roles("ASSESSOR").build(),
                User.withUsername("finance").password(encoder.encode(humanPassword)).roles("FINANCE").build(),
                User.withUsername("system").password(encoder.encode(systemPassword)).roles("SYSTEM").build()));
        for (String username : parseMemberUsernames(memberUsernames)) {
            configuredUsers.add(User.withUsername(username).password(encoder.encode(humanPassword)).roles("MEMBER").build());
        }
        return new InMemoryUserDetailsManager(configuredUsers);
    }

    private static List<String> parseMemberUsernames(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("CLAIMS_MEMBER_USERNAMES must list one or more unique member- identities");
        }
        List<String> usernames = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (String item : configured.split(",", -1)) {
            String username = item.trim();
            if (!username.matches("member-[a-z0-9]+(?:-[a-z0-9]+)*")) {
                throw new IllegalArgumentException("CLAIMS_MEMBER_USERNAMES entries must use the member- prefix and lowercase letters, digits, or hyphens");
            }
            if (username.length() > MAX_MEMBER_USERNAME_LENGTH) {
                throw new IllegalArgumentException("CLAIMS_MEMBER_USERNAMES entries must be at most 127 characters so the member identity fits the database column");
            }
            if (!unique.add(username)) {
                throw new IllegalArgumentException("CLAIMS_MEMBER_USERNAMES entries must be unique");
            }
            usernames.add(username);
        }
        return List.copyOf(usernames);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper json) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/favicon.ico", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(Customizer.withDefaults())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new JsonSecurityHandlers(json))
                        .accessDeniedHandler(new JsonSecurityHandlers(json)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }

    private static void requireConfiguredSecret(String value, String name) {
        if (value == null || value.isBlank() || value.startsWith("CHANGE_ME_"))
            throw new IllegalStateException(name + " must be configured to start the application");
    }
}
