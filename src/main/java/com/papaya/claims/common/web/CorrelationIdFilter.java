package com.papaya.claims.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-ID";
    public static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private final ObservationRegistry observations;

    public CorrelationIdFilter(ObservationRegistry observations) { this.observations = observations; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String correlationId = supplied != null && SAFE.matcher(supplied).matches() ? supplied : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, correlationId);
        response.setHeader(HEADER, correlationId);
        MDC.put("correlationId", correlationId);
        long started = System.nanoTime();
        Observation observation = Observation.createNotStarted("claims.http.filter", observations)
                .lowCardinalityKeyValue("method", request.getMethod());
        observation.start();
        try (Observation.Scope ignored = observation.openScope()) {
            try {
                chain.doFilter(request, response);
            } catch (ServletException | IOException | RuntimeException failure) {
                observation.error(failure);
                throw failure;
            } finally {
                long durationMs = (System.nanoTime() - started) / 1_000_000;
                log.info("http_request method={} path={} status={} durationMs={} correlationId={} traceId={} spanId={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs, correlationId,
                        MDC.get("traceId"), MDC.get("spanId"));
            }
        } finally {
            observation.stop();
            MDC.remove("correlationId");
        }
    }

    public static String currentId(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        return value instanceof String id ? id : UUID.randomUUID().toString();
    }
}
