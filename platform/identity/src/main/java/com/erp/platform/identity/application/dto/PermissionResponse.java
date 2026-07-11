package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Resource;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for permission data.
 *
 * @since 1.0.0
 */
public record PermissionResponse(
    Long id,
    UUID permissionId,
    String permissionCode,
    Resource resource,
    Action action,
    String description,
    PermissionStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
