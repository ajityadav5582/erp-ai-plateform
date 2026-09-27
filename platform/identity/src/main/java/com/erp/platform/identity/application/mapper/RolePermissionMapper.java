package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.RolePermissionResponse;
import com.erp.platform.identity.domain.Permission;
import com.erp.platform.identity.domain.RolePermission;

import java.time.LocalDateTime;

/**
 * Mapper for converting between RolePermission domain entity and DTOs.
 *
 * <p>This is a static utility class that handles the conversion between
 * the domain layer and the application layer, including Instant to LocalDateTime conversion.
 *
 * @since 1.0.0
 */
public final class RolePermissionMapper {

    private RolePermissionMapper() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Converts a RolePermission entity to a RolePermissionResponse DTO.
     *
     * <p>This method enriches the response with permission details
     * (resource, action, description, status, permissionCode).
     *
     * @param rolePermission the role permission entity
     * @param permission the permission entity (for enrichment)
     * @return the response DTO
     */
    public static RolePermissionResponse toResponse(RolePermission rolePermission, Permission permission) {
        if (rolePermission == null) {
            return null;
        }

        return new RolePermissionResponse(
                rolePermission.getId(),
                rolePermission.getRoleId(),
                rolePermission.getPermissionId(),
                permission != null ? permission.getPermissionCode() : null,
                permission != null ? permission.getResourceCode() : null,
                permission != null ? permission.getActionCode() : null,
                permission != null ? permission.getDescription() : null,
                permission != null ? permission.getStatus() : null,
                rolePermission.getAssignedBy(),
                rolePermission.getAssignedAt(),
                rolePermission.isActiveAssignment()
        );
    }

}
