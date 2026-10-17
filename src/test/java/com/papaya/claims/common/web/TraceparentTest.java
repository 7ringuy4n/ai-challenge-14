package com.papaya.claims.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;

class TraceparentTest {
    private static final String TRACE = "0123456789abcdef0123456789abcdef";
    private static final String PARENT = "0123456789abcdef";

    @Test
    void capturesAndRestoresW3cTraceContext() {
        SpanContext remote = SpanContext.createFromRemoteParent(TRACE, PARENT, TraceFlags.getSampled(), TraceState.getDefault());
        try (Scope ignored = Context.root().with(Span.wrap(remote)).makeCurrent()) {
            String value = Traceparent.current();
            assertThat(value).isEqualTo("00-" + TRACE + "-" + PARENT + "-01");
            Context restored = Traceparent.parent(value).orElseThrow();
            assertThat(Span.fromContext(restored).getSpanContext().getTraceId()).isEqualTo(TRACE);
            assertThat(Span.fromContext(restored).getSpanContext().getSpanId()).isEqualTo(PARENT);
            assertThat(Traceparent.ids(value).orElseThrow())
                    .isEqualTo(new Traceparent.Ids(TRACE, PARENT));
        }
    }

    @Test
    void ignoresAbsentMalformedUnsupportedAndInvalidTraceparents() {
        assertThat(Traceparent.current()).isNull();
        assertThat(Traceparent.parent(null)).isEmpty();
        assertThat(Traceparent.parent("not-a-traceparent")).isEmpty();
        assertThat(Traceparent.parent("01-" + TRACE + "-" + PARENT + "-01")).isEmpty();
        assertThat(Traceparent.parent("00-00000000000000000000000000000000-" + PARENT + "-01")).isEmpty();
        assertThat(Traceparent.ids("00-00000000000000000000000000000000-" + PARENT + "-01")).isEmpty();
    }
}
