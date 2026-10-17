package com.papaya.claims.web;

import java.util.Map;

public record ErrorEnvelope(String code, String message, String traceId, Map<String,Object> details) {
    public ErrorEnvelope { details = details == null ? Map.of() : Map.copyOf(details); }
}
