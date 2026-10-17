package com.papaya.claims.claim.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.papaya.claims.claim.domain.PageRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {
    @Test
    void computesPageCountAndCopiesReturnedItems() {
        var items = new ArrayList<>(List.of("a", "b"));
        var page = PageResponse.of(items, 21, new PageRequest(1, 10));
        items.add("outside");

        assertThat(page.items()).containsExactly("a", "b");
        assertThat(page.total()).isEqualTo(21);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void emptyPageHasNoPagesAndRejectsInconsistentMetadata() {
        assertThat(PageResponse.of(List.of(), 0, new PageRequest(0, 20)).totalPages()).isZero();
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 21, 0, 10, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalPages does not match total and size");
    }

    @Test
    void rejectsNegativeAndOutOfRangeMetadataAndGuardsPageCountOverflow() {
        assertThatThrownBy(() -> new PageResponse<>(List.of(), -1, 0, 10, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid page metadata");
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, -1, 10, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid page metadata");
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, 0, 101, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid page metadata");
        assertThatThrownBy(() -> PageResponse.of(List.of(), Long.MAX_VALUE, new PageRequest(0, 1)))
                .isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> new PageResponse<>(List.of("a", "b"), 2, 0, 1, 2))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more items");
    }
}
