package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for assigning a branch to a user.
 *
 * @since 1.0.0
 */
public record AssignBranchRequest(
    /**
     * The ID of the user to assign the branch to.
     */
    @NotNull(message = "User ID is required")
    Long userId,

    /**
     * The ID of the branch to assign.
     */
    @NotNull(message = "Branch ID is required")
    Long branchId,

    /**
     * Whether this branch should be set as the primary branch for the user.
     */
    Boolean isPrimary
) {
}
