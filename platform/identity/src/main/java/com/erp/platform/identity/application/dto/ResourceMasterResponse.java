package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.ResourceStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for resource (module) data.
 *
 * @since 1.0.0
 */
public record ResourceMasterResponse(
    Long id,
    String resourceCode,
    String resourceName,
    String module,
    String description,
    ResourceStatus status,
    Boolean isSystem,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
