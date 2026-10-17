package com.papaya.claims.common.web;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts active W3C trace context to/from the durable local-outbox carrier. */
public final class Traceparent {
    private static final Pattern FORMAT = Pattern.compile("00-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})");
    private Traceparent() { }

    public static String current() {
        SpanContext context = Span.current().getSpanContext();
        if (!context.isValid()) return null;
        return "00-" + context.getTraceId() + "-" + context.getSpanId() + "-" + context.getTraceFlags().asHex();
    }

    public static Optional<Ids> ids(String value) {
        if (value == null) return Optional.empty();
        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) return Optional.empty();
        SpanContext parsed = SpanContext.createFromRemoteParent(matcher.group(1), matcher.group(2),
                TraceFlags.fromHex(matcher.group(3), 0), TraceState.getDefault());
        return parsed.isValid() ? Optional.of(new Ids(parsed.getTraceId(), parsed.getSpanId())) : Optional.empty();
    }

    public static Optional<Context> parent(String value) {
        return ids(value).map(ids -> {
            SpanContext parsed = SpanContext.createFromRemoteParent(ids.traceId(), ids.parentSpanId(),
                    TraceFlags.fromHex(value.substring(value.length() - 2), 0), TraceState.getDefault());
            return Context.root().with(Span.wrap(parsed));
        });
    }

    public record Ids(String traceId, String parentSpanId) { }
}
