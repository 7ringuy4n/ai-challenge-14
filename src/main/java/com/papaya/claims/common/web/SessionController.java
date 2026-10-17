package com.papaya.claims.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/session")
public class SessionController {
    @GetMapping
    Map<String, String> session(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrf) return Map.of("csrfHeaderName", csrf.getHeaderName(), "csrfParameterName", csrf.getParameterName(), "csrfToken", csrf.getToken());
        return Map.of();
    }
}
