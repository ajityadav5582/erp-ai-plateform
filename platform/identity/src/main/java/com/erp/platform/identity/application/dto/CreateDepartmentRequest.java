package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new department.
 *
 * @since 1.0.0
 */
public record CreateDepartmentRequest(
    @NotBlank(message = "Department code is required")
    @Size(min = 1, max = 50, message = "Department code must be between 1 and 50 characters")
    String departmentCode,

    @NotBlank(message = "Department name is required")
    @Size(min = 1, max = 200, message = "Department name must be between 1 and 200 characters")
    String departmentName,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @NotNull(message = "Branch ID is required")
    Long branchId,

    Long managerId,

    Long parentDepartmentId
) {
}
