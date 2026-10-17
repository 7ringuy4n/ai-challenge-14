package com.papaya.claims.web;

import static org.assertj.core.api.Assertions.assertThat;
import com.papaya.claims.contract.BusinessException;
import com.papaya.claims.observability.TraceContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {
    @Test void businessErrorPreservesStableStatusAndSafeDomainMessageWithTrace() {
        var request = new MockHttpServletRequest(); request.setAttribute(TraceContextFilter.TRACE_ID_ATTRIBUTE, "trace-abc");
        var response = new GlobalExceptionHandler().business(new BusinessException("CONFLICT", 409, "Claim version is stale"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isEqualTo(new ErrorEnvelope("CONFLICT", "Claim version is stale", "trace-abc", java.util.Map.of()));
    }
    @Test void unexpectedErrorDoesNotExposeExceptionOrInputDetails() {
        var request = new MockHttpServletRequest();
        var response = new GlobalExceptionHandler().unexpected(new IllegalStateException("password=secret SQL SELECT"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("The request could not be completed");
        assertThat(response.getBody().toString()).doesNotContain("secret", "SELECT", "IllegalStateException");
    }
}
