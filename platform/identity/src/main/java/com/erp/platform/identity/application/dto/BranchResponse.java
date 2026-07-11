package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.BranchStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for branch data.
 *
 * @since 1.0.0
 */
public record BranchResponse(
    Long id,
    UUID branchId,
    Long tenantId,
    String branchCode,
    String branchName,
    String email,
    String phone,
    String address,
    String city,
    String state,
    String country,
    String postalCode,
    String timezone,
    String currency,
    Long managerId,
    BranchStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
