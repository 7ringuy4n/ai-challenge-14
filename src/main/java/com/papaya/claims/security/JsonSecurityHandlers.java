package com.papaya.claims.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.api.dto.ApiError;
import com.papaya.claims.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

public final class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper json;
    public JsonSecurityHandlers(ObjectMapper json) { this.json = json; }

    @Override public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"Claims Workflow\"");
        write(request, response, 401, "UNAUTHENTICATED", "Authentication is required");
    }
    @Override public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        write(request, response, 403, "FORBIDDEN", "The authenticated actor cannot perform this operation");
    }
    private void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), new ApiError(code, message, List.of(), CorrelationIdFilter.currentId(request)));
    }
}
