package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing permission.
 *
 * @since 1.0.0
 */
public record UpdatePermissionRequest(
    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description
) {
}
