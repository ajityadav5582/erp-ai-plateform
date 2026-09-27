package com.erp.platform.identity.application.dto;

import java.util.List;

/**
 * Response DTO for paginated list of user-branch assignments.
 *
 * @since 1.0.0
 */
public record UserBranchListResponse(
    /**
     * The list of user-branch assignments.
     */
    List<UserBranchResponse> assignments,

    /**
     * The current page number (0-indexed).
     */
    int page,

    /**
     * The number of items per page.
     */
    int size,

    /**
     * The total number of assignments.
     */
    long totalElements,

    /**
     * The total number of pages.
     */
    int totalPages,

    /**
     * Whether this is the last page.
     */
    boolean last
) {
}
