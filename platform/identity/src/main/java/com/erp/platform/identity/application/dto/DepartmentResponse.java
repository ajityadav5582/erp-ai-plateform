package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.DepartmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for department data, including parent department information.
 *
 * @since 1.0.0
 */
public record DepartmentResponse(
    Long id,
    UUID departmentId,
    Long tenantId,
    Long branchId,
    String departmentCode,
    String departmentName,
    String description,
    Long managerId,
    Long parentDepartmentId,
    DepartmentStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
