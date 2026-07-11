package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Resource;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for role permission assignment.
 *
 * @since 1.0.0
 */
public record RolePermissionResponse(
        Long id,
        UUID rolePermissionId,
        Long roleId,
        Long permissionId,
        String permissionCode,
        Resource resource,
        Action action,
        String description,
        PermissionStatus status,
        String assignedBy,
        LocalDateTime assignedAt,
        Integer version
) {
}
