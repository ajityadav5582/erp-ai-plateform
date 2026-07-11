package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing role.
 *
 * @since 1.0.0
 */
public record UpdateRoleRequest(
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    String roleName,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description
) {
}
