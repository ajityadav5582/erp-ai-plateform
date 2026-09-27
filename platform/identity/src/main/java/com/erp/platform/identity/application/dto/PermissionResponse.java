package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.PermissionStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for permission data.
 *
 * @since 1.0.0
 */
public record PermissionResponse(
    Long id,
    String permissionCode,
    String permissionName,
    String resource,
    String action,
    String description,
    PermissionStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
