package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new permission.
 *
 * @since 1.0.0
 */
public record CreatePermissionRequest(
    @NotNull(message = "Resource code is required")
    String resourceCode,

    @NotNull(message = "Action code is required")
    String actionCode,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description
) {
}
