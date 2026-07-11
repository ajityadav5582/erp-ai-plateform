package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.DepartmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for department list data (summary view).
 *
 * @since 1.0.0
 */
public record DepartmentListResponse(
    Long id,
    UUID departmentId,
    String departmentCode,
    String departmentName,
    Long branchId,
    Long parentDepartmentId,
    Long managerId,
    DepartmentStatus status,
    LocalDateTime createdAt
) {
}
