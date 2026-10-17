package com.papaya.claims.claim.api.dto;

import com.papaya.claims.claim.domain.PageRequest;
import com.papaya.claims.claim.domain.PageResult;
import java.util.List;
import java.util.Objects;

public record PageResponse<T>(List<T> items, long total, int page, int size, int totalPages) {
    public PageResponse {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (total < 0 || page < 0 || size < 1 || size > PageRequest.MAX_SIZE || totalPages < 0) {
            throw new IllegalArgumentException("Invalid page metadata");
        }
        int expectedPages = total == 0 ? 0 : Math.toIntExact((total + size - 1) / size);
        if (totalPages != expectedPages) throw new IllegalArgumentException("totalPages does not match total and size");
        if (items.size() > size) throw new IllegalArgumentException("Page contains more items than size");
    }

    public static <T> PageResponse<T> of(List<T> items, long total, PageRequest request) {
        int pages = total == 0 ? 0 : Math.toIntExact((total + request.size() - 1) / request.size());
        return new PageResponse<>(items, total, request.page(), request.size(), pages);
    }

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return of(result.items(), result.total(), result.request());
    }
}
