package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.ResourceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a resource (module) entry.
 *
 * @since 1.0.0
 */
public record CreateResourceMasterRequest(
    @NotBlank(message = "Resource code is required")
    @Size(max = 50, message = "Resource code must not exceed 50 characters")
    String resourceCode,

    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    String resourceName,

    @NotBlank(message = "Module is required")
    @Size(max = 50, message = "Module must not exceed 50 characters")
    String module,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    ResourceStatus status,

    Boolean isSystem
) {
}
