package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BranchStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for branch list data (summary view).
 *
 * @since 1.0.0
 */
public record BranchListResponse(
    Long id,
    UUID branchId,
    String branchCode,
    String branchName,
    String city,
    String country,
    Long managerId,
    BranchStatus status,
    LocalDateTime createdAt
) {
}
