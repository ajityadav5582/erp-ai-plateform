package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.PermissionStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for role permission assignment.
 *
 * @since 1.0.0
 */
public record RolePermissionResponse(
        Long id,
        Long roleId,
        Long permissionId,
        String permissionCode,
        String resource,
        String action,
        String description,
        PermissionStatus status,
        String assignedBy,
        LocalDateTime assignedAt,
        boolean active
) {
}
