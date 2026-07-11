package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing department.
 *
 * <p>All fields are optional; only non-null fields are applied during update.
 *
 * @since 1.0.0
 */
public record UpdateDepartmentRequest(
    @Size(min = 1, max = 50, message = "Department code must be between 1 and 50 characters")
    String departmentCode,

    @Size(min = 1, max = 200, message = "Department name must be between 1 and 200 characters")
    String departmentName,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    Long branchId,

    Long managerId,

    Long parentDepartmentId
) {
}
