package com.papaya.claims.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    @Test
    void preservesOnlyBoundedSafeIdsAndReturnsTheEffectiveCorrelationId() throws Exception {
        var filter = new CorrelationIdFilter();
        var request = new MockHttpServletRequest("GET", "/api/v1/claims");
        request.addHeader("X-Correlation-ID", "client:trace-17");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(MDC.get("correlationId")).isEqualTo("client:trace-17"));
        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("client:trace-17");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void replacesMalformedOrOversizedIdsWithGeneratedUuid() throws Exception {
        var filter = new CorrelationIdFilter();
        var request = new MockHttpServletRequest("GET", "/api/v1/claims");
        request.addHeader("X-Correlation-ID", "bad value");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> { });
        String effective = response.getHeader("X-Correlation-ID");
        assertThat(effective).matches("[0-9a-f-]{36}");
    }

    @Test
    void removesMdcWhenDownstreamFails() throws Exception {
        var filter = new CorrelationIdFilter();
        var request = new MockHttpServletRequest("POST", "/api/v1/claims");
        request.addHeader("X-Correlation-ID", "valid-17");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new java.io.IOException("downstream failed");
        })).isInstanceOf(java.io.IOException.class).hasMessage("downstream failed");
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("valid-17");
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/claims"),
                new MockHttpServletResponse(), (req, res) -> { throw new IllegalStateException("downstream runtime failure"); }))
                .isInstanceOf(IllegalStateException.class).hasMessage("downstream runtime failure");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void currentIdUsesExistingRequestAttributeAndGeneratesWhenAbsent() {
        var request = new MockHttpServletRequest();
        assertThat(CorrelationIdFilter.currentId(request)).matches("[0-9a-f-]{36}");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, "trusted-id");
        assertThat(CorrelationIdFilter.currentId(request)).isEqualTo("trusted-id");
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, 4);
        assertThat(CorrelationIdFilter.currentId(request)).matches("[0-9a-f-]{36}");
    }
}
