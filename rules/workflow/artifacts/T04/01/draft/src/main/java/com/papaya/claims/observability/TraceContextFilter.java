package com.papaya.claims.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Carries a bounded trace identifier through audit and structured application logs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class TraceContextFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader("X-Trace-Id");
        String traceId = incoming != null && incoming.matches("[A-Za-z0-9._-]{1,128}") ? incoming : UUID.randomUUID().toString();
        MDC.put("traceId", traceId); response.setHeader("X-Trace-Id", traceId);
        try { chain.doFilter(request, response); }
        finally { MDC.remove("traceId"); }
    }
}
