package com.papaya.claims.web;

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JsonRequestSizeFilterTest {
    @Test void rejectsOversizedJsonEvenWhenLengthWasNotDeclared() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/claims"); request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(new byte[JsonRequestSizeFilter.MAX_BYTES + 1]);
        var response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { throw new AssertionError("Oversized JSON must not reach the controller"); };
        new JsonRequestSizeFilter().doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("REQUEST_TOO_LARGE").contains("64 KiB");
    }
}
