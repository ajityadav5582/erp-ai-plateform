package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BranchStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for the current user's accessible branches in a branch switcher.
 *
 * @since 1.0.0
 */
public record BranchMyInfoResponse(
    /**
     * The unique identifier of the branch.
     */
    Long id,

    /**
     * The company this branch belongs to.
     */
    Long companyId,

    /**
     * The branch code.
     */
    String branchCode,

    /**
     * The branch name.
     */
    String branchName,

    /**
     * The status of the branch.
     */
    BranchStatus status,

    /**
     * Whether this branch is the primary branch for the current user.
     */
    boolean isPrimary
) {
}
