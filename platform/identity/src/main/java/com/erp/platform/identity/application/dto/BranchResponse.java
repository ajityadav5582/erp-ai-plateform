package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BranchStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for branch data.
 *
 * <p>A branch carries a free-text address, ward number, and administrative geography.
 *
 * @since 1.0.0
 */
public record BranchResponse(
    Long id,
    Long tenantId,
    Long companyId,
    String branchCode,
    String branchName,
    String address,
    String wardNo,
    boolean isHeadOffice,
    Short provinceId,
    Short districtId,
    Short localLevelId,
    Long defaultWarehouseId,
    BranchStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
