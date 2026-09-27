package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BranchStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for branch data.
 *
 * <p>A branch's location is derived from a local level (province -> district ->
 * local level) referenced via {@code localLevelId}. An optional {@code wardNo}
 * can further qualify the branch address.
 *
 * @since 1.0.0
 */
public record BranchResponse(
    Long id,
    Long tenantId,
    String branchCode,
    String branchName,
    String email,
    String phone,
    String address,
    String localLevelId,
    String wardNo,
    BranchStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
