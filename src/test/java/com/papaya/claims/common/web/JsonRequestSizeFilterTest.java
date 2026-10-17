package com.papaya.claims.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JsonRequestSizeFilterTest {
    @Test
    void rejectsOversizedJsonEvenWhenContentLengthIsUnknown() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/claims");
        request.setContentType("application/json");
        request.setContent(new byte[JsonRequestSizeFilter.MAX_BYTES + 1]);
        var response = new MockHttpServletResponse();
        new JsonRequestSizeFilter(new ObjectMapper()).doFilter(request, response, (req, res) -> {
            throw new AssertionError("Oversized request must be stopped before controller dispatch");
        });
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("REQUEST_TOO_LARGE");
    }

    @Test
    void passesAWithinLimitBodyToTheNextFilterUnchanged() throws Exception {
        byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
        var request = new MockHttpServletRequest("POST", "/api/v1/claims");
        request.setContentType("application/json"); request.setContent(body);
        var response = new MockHttpServletResponse();
        boolean[] called = {false};
        FilterChain chain = (req, res) -> {
            called[0] = true;
            assertThat(req.getInputStream().readAllBytes()).containsExactly(body);
        };
        new JsonRequestSizeFilter(new ObjectMapper()).doFilter(request, response, chain);
        assertThat(called[0]).isTrue();
    }

    @Test
    void onlyInspectsJsonApiRequestsAndCachedBodySupportsReaderAndRepeatedStreams() throws Exception {
        var filter = new JsonRequestSizeFilter(new ObjectMapper());
        var request = new MockHttpServletRequest("POST", "/api/v1/claims");
        byte[] body = "{\"name\":\"claim\"}".getBytes(StandardCharsets.UTF_8);
        request.setContentType("application/json; charset=utf-8");
        request.setContent(body);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            var wrapped = (jakarta.servlet.http.HttpServletRequest) req;
            assertThat(wrapped.getContentLength()).isEqualTo(body.length);
            assertThat(wrapped.getContentLengthLong()).isEqualTo(body.length);
            assertThat(wrapped.getReader().readLine()).isEqualTo(new String(body, StandardCharsets.UTF_8));
            var stream = wrapped.getInputStream();
            assertThat(stream.isFinished()).isFalse();
            assertThat(stream.readAllBytes()).containsExactly(body);
            assertThat(stream.isReady()).isTrue();
            assertThat(stream.isFinished()).isTrue();
            assertThatThrownBy(() -> stream.setReadListener(null)).isInstanceOf(UnsupportedOperationException.class);
        });
        assertThat(response.getStatus()).isEqualTo(200);

        var nonApi = new MockHttpServletRequest("POST", "/outside");
        nonApi.setContentType("application/json");
        nonApi.setContent(body);
        boolean[] called = {false};
        filter.doFilter(nonApi, new MockHttpServletResponse(), (req, res) -> called[0] = true);
        assertThat(called[0]).isTrue();

        var noContentType = new MockHttpServletRequest("POST", "/api/v1/claims");
        noContentType.setContent(body);
        called[0] = false;
        filter.doFilter(noContentType, new MockHttpServletResponse(), (req, res) -> called[0] = true);
        assertThat(called[0]).isTrue();

        var nonJson = new MockHttpServletRequest("POST", "/api/v1/claims");
        nonJson.setContentType("text/plain");
        nonJson.setContent(body);
        called[0] = false;
        filter.doFilter(nonJson, new MockHttpServletResponse(), (req, res) -> called[0] = true);
        assertThat(called[0]).isTrue();
    }
}
