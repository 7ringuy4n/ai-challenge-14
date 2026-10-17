package com.papaya.claims.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(PasswordEncoder encoder,
            @Value("${claims.demo-password}") String humanPassword,
            @Value("${claims.system-password}") String systemPassword) {
        if (humanPassword == null || humanPassword.isBlank() || systemPassword == null || systemPassword.isBlank() || humanPassword.equals(systemPassword))
            throw new IllegalStateException("Separate nonempty DEMO_PASSWORD and SYSTEM_PASSWORD values are required");
        return new InMemoryUserDetailsManager(
                User.withUsername("clerk").password(encoder.encode(humanPassword)).roles("DOCUMENT_CLERK").build(),
                User.withUsername("lead").password(encoder.encode(humanPassword)).roles("TEAM_LEAD").build(),
                User.withUsername("assessor").password(encoder.encode(humanPassword)).roles("ASSESSOR").build(),
                User.withUsername("finance").password(encoder.encode(humanPassword)).roles("FINANCE").build(),
                User.withUsername("member-001").password(encoder.encode(humanPassword)).roles("MEMBER").build(),
                User.withUsername("system").password(encoder.encode(systemPassword)).roles("SYSTEM").build());
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        http.csrf(c -> c.csrfTokenRepository(csrf))
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(a -> a.requestMatchers("/", "/index.html", "/static/**", "/app.js", "/styles.css", "/favicon.ico", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/system/**").hasRole("SYSTEM")
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/actuator/**").authenticated().anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(response.getOutputStream(), Map.of("code", "UNAUTHENTICATED", "message", "Authentication is required", "traceId", trace(request), "details", Map.of()));
                }).accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    json.writeValue(response.getOutputStream(), Map.of("code", "FORBIDDEN", "message", "Access is denied", "traceId", trace(request), "details", Map.of()));
                }));
        return http.build();
    }
    private static String trace(jakarta.servlet.http.HttpServletRequest request) {
        Object value = request.getAttribute(com.papaya.claims.observability.TraceContextFilter.TRACE_ID_ATTRIBUTE);
        return value instanceof String traceId ? traceId : "unavailable";
    }
}
