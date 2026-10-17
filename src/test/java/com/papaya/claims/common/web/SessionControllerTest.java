package com.papaya.claims.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class SessionControllerTest {
    private final SessionController controller = new SessionController();

    @Test
    void returnsNoCsrfFieldsWhenSecurityDidNotProvideAToken() {
        assertThat(controller.session(new MockHttpServletRequest())).isEmpty();
    }

    @Test
    void exposesTheCsrfHeaderParameterAndTokenForAuthenticatedSessions() {
        var request = new MockHttpServletRequest();
        request.setAttribute("org.springframework.security.web.csrf.CsrfToken", new DefaultCsrfToken("X-CSRF", "_csrf", "token-value"));
        assertThat(controller.session(request)).containsEntry("csrfHeaderName", "X-CSRF")
                .containsEntry("csrfParameterName", "_csrf").containsEntry("csrfToken", "token-value");
    }
}
