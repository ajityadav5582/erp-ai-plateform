package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BranchStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for branch list data (summary view).
 *
 * @since 1.0.0
 */
public record BranchListResponse(
    Long id,
    String branchCode,
    String branchName,
    String localLevelId,
    BranchStatus status,
    LocalDateTime createdAt
) {
}
