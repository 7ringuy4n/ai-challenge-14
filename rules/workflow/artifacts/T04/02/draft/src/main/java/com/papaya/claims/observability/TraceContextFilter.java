package com.papaya.claims.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Exposes the active Micrometer trace/span context to audits, responses, and structured logs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class TraceContextFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_ATTRIBUTE = TraceContextFilter.class.getName() + ".traceId";
    public static final String SPAN_ID_ATTRIBUTE = TraceContextFilter.class.getName() + ".spanId";
    private static final Logger LOG = LoggerFactory.getLogger(TraceContextFilter.class);
    private final Tracer tracer;

    public TraceContextFilter(Tracer tracer) { this.tracer = tracer; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Span span = tracer.currentSpan();
        boolean created = span == null;
        Tracer.SpanInScope scope = null;
        if (created) {
            span = tracer.nextSpan().name("claims.http").start();
            scope = tracer.withSpan(span);
        }
        String traceId = span.context().traceId();
        String spanId = span.context().spanId();
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        request.setAttribute(SPAN_ID_ATTRIBUTE, spanId);
        response.setHeader("X-Trace-Id", traceId);
        response.setHeader("X-Span-Id", spanId);
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            LOG.info("http_request method={} path={} status={} durationMs={} traceId={} spanId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs, traceId, spanId);
            MDC.remove("traceId");
            MDC.remove("spanId");
            if (scope != null) scope.close();
            if (created) span.end();
        }
    }
}
