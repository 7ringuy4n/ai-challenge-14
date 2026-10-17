package com.papaya.claims.observability;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceContextFilterTest {
    @AfterEach void clearMdc() { MDC.clear(); }

    @Test void reusesCurrentSpanAndExposesTraceContextOnlyDuringTheChain() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = span("existing-trace", "existing-span");
        when(tracer.currentSpan()).thenReturn(span);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new TraceContextFilter(tracer).doFilter(request, response, (req, res) -> {
            assertThat(req.getAttribute(TraceContextFilter.TRACE_ID_ATTRIBUTE)).isEqualTo("existing-trace");
            assertThat(req.getAttribute(TraceContextFilter.SPAN_ID_ATTRIBUTE)).isEqualTo("existing-span");
            assertThat(((MockHttpServletResponse) res).getHeader("X-Trace-Id")).isEqualTo("existing-trace");
            assertThat(((MockHttpServletResponse) res).getHeader("X-Span-Id")).isEqualTo("existing-span");
            assertThat(MDC.get("traceId")).isEqualTo("existing-trace");
            assertThat(MDC.get("spanId")).isEqualTo("existing-span");
        });

        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
        verify(tracer, never()).nextSpan();
        verify(tracer, never()).withSpan(any());
        verify(span, never()).end();
    }

    @Test void createsAndClosesSpanAndCleansMdcWhenFilterChainThrows() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = span("created-trace", "created-span");
        Tracer.SpanInScope scope = mock(Tracer.SpanInScope.class);
        when(tracer.currentSpan()).thenReturn(null);
        when(tracer.nextSpan()).thenReturn(span);
        when(tracer.withSpan(span)).thenReturn(scope);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = request();
        ServletException failure = new ServletException("controller failed");

        assertThatThrownBy(() -> new TraceContextFilter(tracer).doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("traceId")).isEqualTo("created-trace");
            throw failure;
        })).isSameAs(failure);

        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("created-trace");
        verify(scope).close();
        verify(span).end();
    }

    @Test void propagatesIoFailureAfterRestoringMdcAndClosingCreatedScope() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = span("trace-io", "span-io");
        Tracer.SpanInScope scope = mock(Tracer.SpanInScope.class);
        when(tracer.currentSpan()).thenReturn(null);
        when(tracer.nextSpan()).thenReturn(span);
        when(tracer.withSpan(span)).thenReturn(scope);
        IOException failure = new IOException("client disconnected");
        assertThatThrownBy(() -> new TraceContextFilter(tracer).doFilter(request(), new MockHttpServletResponse(), (req, res) -> { throw failure; }))
                .isSameAs(failure);
        assertThat(MDC.get("traceId")).isNull();
        verify(scope).close();
        verify(span).end();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET"); request.setRequestURI("/api/claims");
        return request;
    }

    private static Span span(String traceId, String spanId) {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(context.traceId()).thenReturn(traceId);
        when(context.spanId()).thenReturn(spanId);
        when(span.context()).thenReturn(context);
        when(span.name("claims.http")).thenReturn(span);
        when(span.start()).thenReturn(span);
        return span;
    }
}
