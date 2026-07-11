package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.Resource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new permission.
 *
 * @since 1.0.0
 */
public record CreatePermissionRequest(
    @NotNull(message = "Resource is required")
    Resource resource,

    @NotNull(message = "Action is required")
    Action action,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description
) {
}
