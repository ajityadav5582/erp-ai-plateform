package com.erp.platform.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PageResponse}.
 *
 * @since 1.0.0
 */
class PageResponseTest {

    @Test
    void of_shouldCreatePageResponse() {
        List<String> content = List.of("item1", "item2", "item3");
        PageResponse.Pagination pagination = PageResponse.Pagination.of(0, 20, 3, 1);

        PageResponse<String> response = PageResponse.of(content, pagination);

        assertThat(response.content()).isEqualTo(content);
        assertThat(response.pagination()).isEqualTo(pagination);
    }

    @Test
    void pagination_of_shouldCreatePagination() {
        PageResponse.Pagination pagination = PageResponse.Pagination.of(0, 20, 100, 5);

        assertThat(pagination.page()).isEqualTo(0);
        assertThat(pagination.size()).isEqualTo(20);
        assertThat(pagination.totalElements()).isEqualTo(100);
        assertThat(pagination.totalPages()).isEqualTo(5);
        assertThat(pagination.first()).isTrue();
        assertThat(pagination.last()).isFalse();
    }

    @Test
    void pagination_of_shouldSetFirstAndLastCorrectly() {
        PageResponse.Pagination firstPage = PageResponse.Pagination.of(0, 20, 100, 5);
        PageResponse.Pagination lastPage = PageResponse.Pagination.of(4, 20, 100, 5);

        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();
        assertThat(lastPage.first()).isFalse();
        assertThat(lastPage.last()).isTrue();
    }

    @Test
    void pagination_of_shouldHandleSinglePage() {
        PageResponse.Pagination pagination = PageResponse.Pagination.of(0, 20, 10, 1);

        assertThat(pagination.first()).isTrue();
        assertThat(pagination.last()).isTrue();
    }
}
