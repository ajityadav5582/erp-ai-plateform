package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for assigning a department to a user.
 *
 * @param userId the user ID
 * @param departmentId the department ID
 * @param isPrimary whether this is the primary department for the user
 * @since 1.0.0
 */
public record AssignDepartmentRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Department ID is required")
        Long departmentId,

        Boolean isPrimary
) {
}
