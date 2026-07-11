package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new role.
 *
 * @since 1.0.0
 */
public record CreateRoleRequest(
    @NotBlank(message = "Role code is required")
    @Size(min = 2, max = 100, message = "Role code must be between 2 and 100 characters")
    String roleCode,

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 100, message = "Role name must be between 2 and 100 characters")
    String roleName,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description
) {
}
