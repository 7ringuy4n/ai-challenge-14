package com.papaya.claims.claim.api.dto;

import java.util.List;
import java.util.Objects;

public record ApiError(String code, String message, List<String> details, String correlationId) {
    public ApiError {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Error code is required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Error message is required");
        details = List.copyOf(Objects.requireNonNullElse(details, List.of()));
        correlationId = Objects.requireNonNullElse(correlationId, "");
    }
}
