package com.papaya.claims.claim.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable JSON object values. Nested maps and lists are defensively copied. */
public record ClaimFacts(Map<String, Object> values) {
    public ClaimFacts {
        Objects.requireNonNull(values, "values");
        values = immutableMap(values);
    }

    private static Map<String, Object> immutableMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (!(key instanceof String stringKey) || stringKey.isBlank()) {
                throw new IllegalArgumentException("Fact keys must be nonblank strings");
            }
            copy.put(stringKey, immutableValue(value));
        });
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof BigDecimal
                || value instanceof Integer || value instanceof Long) return value;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        if (value instanceof Map<?, ?> map) return immutableMap(map);
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(item -> copy.add(immutableValue(item)));
            return Collections.unmodifiableList(copy);
        }
        throw new IllegalArgumentException("Unsupported claim fact value type: " + value.getClass().getName());
    }
}
