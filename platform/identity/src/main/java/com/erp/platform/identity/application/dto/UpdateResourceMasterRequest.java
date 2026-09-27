package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.ResourceStatus;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a resource (module) entry.
 *
 * @since 1.0.0
 */
public record UpdateResourceMasterRequest(
    @Size(max = 50, message = "Resource code must not exceed 50 characters")
    String resourceCode,

    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    String resourceName,

    @Size(max = 50, message = "Module must not exceed 50 characters")
    String module,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    ResourceStatus status,

    Boolean isSystem
) {
}
