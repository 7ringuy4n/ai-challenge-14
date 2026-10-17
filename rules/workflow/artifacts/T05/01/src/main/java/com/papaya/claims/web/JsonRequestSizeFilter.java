package com.papaya.claims.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Buffers only API JSON bodies and enforces the same limit for fixed and chunked requests. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public final class JsonRequestSizeFilter extends OncePerRequestFilter {
    public static final int MAX_BYTES = 64 * 1024;
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String contentType = request.getContentType();
        if (!request.getRequestURI().startsWith("/api/") || contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            chain.doFilter(request, response); return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) {
            response.setStatus(400); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getOutputStream(),
                    new ErrorEnvelope("REQUEST_TOO_LARGE", "JSON request body exceeds 64 KiB", trace(request), java.util.Map.of()));
            return;
        }
        chain.doFilter(new BufferedRequest(request, body), response);
    }
    private static String trace(HttpServletRequest request) {
        Object value = request.getAttribute(com.papaya.claims.observability.TraceContextFilter.TRACE_ID_ATTRIBUTE);
        return value instanceof String text ? text : "unavailable";
    }
    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        BufferedRequest(HttpServletRequest request, byte[] body) { super(request); this.body = body; }
        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }
        @Override public jakarta.servlet.ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new jakarta.servlet.ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(jakarta.servlet.ReadListener listener) { throw new UnsupportedOperationException(); }
            };
        }
        @Override public java.io.BufferedReader getReader() { return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), getCharacterEncoding() == null ? java.nio.charset.StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(getCharacterEncoding()))); }
    }
}
