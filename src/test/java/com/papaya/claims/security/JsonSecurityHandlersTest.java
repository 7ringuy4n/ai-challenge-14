package com.papaya.claims.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.api.dto.ApiError;
import com.papaya.claims.common.web.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JsonSecurityHandlersTest {
    private final JsonSecurityHandlers handlers = new JsonSecurityHandlers(new ObjectMapper());

    @Test
    void authenticationFailureReturnsJson401AndBasicChallenge() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/claims");
        var response = new MockHttpServletResponse();
        handlers.commence(request, response, new BadCredentialsException("secret detail"));
        ApiError error = new ObjectMapper().readValue(response.getContentAsByteArray(), ApiError.class);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Basic realm=\"Claims Workflow\"");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(error.code()).isEqualTo("UNAUTHENTICATED");
        assertThat(error.message()).isEqualTo("Authentication is required");
        assertThat(error.message()).doesNotContain("secret detail");
    }

    @Test
    void accessDeniedReturnsSafeJsonAndPropagatesCorrelationId() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/claims");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, "trace-123");
        var response = new MockHttpServletResponse();
        handlers.handle(request, response, new AccessDeniedException("private policy detail"));
        ApiError error = new ObjectMapper().readValue(response.getContentAsByteArray(), ApiError.class);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(error.code()).isEqualTo("FORBIDDEN");
        assertThat(error.correlationId()).isEqualTo("trace-123");
        assertThat(error.message()).doesNotContain("private policy detail");
    }
}
