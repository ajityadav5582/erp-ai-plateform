package com.erp.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * Standard paginated response wrapper.
 *
 * @param <T> the type of elements in the page
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Paginated response")
public record PageResponse<T>(
    @Schema(description = "Page content")
    List<T> content,

    @Schema(description = "Pagination metadata")
    Pagination pagination
) implements Serializable {

    /**
     * Creates a PageResponse from content and pagination info.
     */
    public static <T> PageResponse<T> of(List<T> content, Pagination pagination) {
        return new PageResponse<>(content, pagination);
    }

    /**
     * Pagination metadata.
     *
     * @param page the current page number (0-based)
     * @param size the page size
     * @param totalElements the total number of elements
     * @param totalPages the total number of pages
     * @param first whether this is the first page
     * @param last whether this is the last page
     */
    public record Pagination(
        @Schema(description = "Current page number (0-based)", example = "0")
        int page,

        @Schema(description = "Page size", example = "20")
        int size,

        @Schema(description = "Total number of elements", example = "150")
        long totalElements,

        @Schema(description = "Total number of pages", example = "8")
        int totalPages,

        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,

        @Schema(description = "Whether this is the last page", example = "false")
        boolean last
    ) implements Serializable {

        /**
         * Creates pagination metadata.
         */
        public static Pagination of(int page, int size, long totalElements, int totalPages) {
            return new Pagination(page, size, totalElements, totalPages, page == 0,
                page >= totalPages - 1);
        }
    }
}
