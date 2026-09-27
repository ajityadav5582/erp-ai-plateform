package com.erp.platform.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user-branch assignment information.
 *
 * @since 1.0.0
 */
public record UserBranchResponse(
    /**
     * The unique identifier of the user-branch assignment.
     */
    UUID userBranchId,

    /**
     * The ID of the user.
     */
    Long userId,

    /**
     * The username of the user.
     */
    String username,

    /**
     * The ID of the branch.
     */
    Long branchId,

    /**
     * The code of the branch.
     */
    String branchCode,

    /**
     * The name of the branch.
     */
    String branchName,

    /**
     * Whether this branch is the primary branch for the user.
     */
    Boolean isPrimary,

    /**
     * The user who assigned this branch.
     */
    String assignedBy,

    /**
     * The timestamp when the branch was assigned.
     */
    Instant assignedAt
) {
}
