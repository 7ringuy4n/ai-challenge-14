package com.papaya.claims.claim.domain;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(List<T> items, long total, PageRequest request) {
    public PageResult {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (total < 0) throw new IllegalArgumentException("Page total cannot be negative");
        Objects.requireNonNull(request, "request");
        if (items.size() > request.size()) throw new IllegalArgumentException("Page contains more items than requested");
    }
}
