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

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

    @Bean
    InMemoryUserDetailsManager users(PasswordEncoder encoder,
            @Value("${claims.security.demo-password:}") String humanPassword,
            @Value("${claims.security.system-password:}") String systemPassword) {
        requireConfiguredSecret(humanPassword, "CLAIMS_DEMO_PASSWORD");
        requireConfiguredSecret(systemPassword, "CLAIMS_SYSTEM_PASSWORD");
        return new InMemoryUserDetailsManager(
                User.withUsername("clerk").password(encoder.encode(humanPassword)).roles("DOCUMENT_CLERK").build(),
                User.withUsername("lead").password(encoder.encode(humanPassword)).roles("TEAM_LEAD").build(),
                User.withUsername("assessor").password(encoder.encode(humanPassword)).roles("ASSESSOR").build(),
                User.withUsername("finance").password(encoder.encode(humanPassword)).roles("FINANCE").build(),
                User.withUsername("member-demo").password(encoder.encode(humanPassword)).roles("MEMBER").build(),
                User.withUsername("system").password(encoder.encode(systemPassword)).roles("SYSTEM").build());
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
