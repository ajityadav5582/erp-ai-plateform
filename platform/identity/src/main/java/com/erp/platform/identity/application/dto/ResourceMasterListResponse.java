package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.ResourceStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for resource list data (summary view).
 *
 * @since 1.0.0
 */
public record ResourceMasterListResponse(
    Long id,
    String resourceCode,
    String resourceName,
    String module,
    ResourceStatus status,
    Boolean isSystem,
    LocalDateTime createdAt
) {
}
