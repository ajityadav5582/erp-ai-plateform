package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.DataScope;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for assigning a permission to a role.
 *
 * @since 1.0.0
 */
public record AssignPermissionRequest(
        @NotNull(message = "Role ID is required")
        Long roleId,

        @NotNull(message = "Permission ID is required")
        Long permissionId,

        String assignedBy,

        DataScope dataScope
) {

    /**
     * Backward-compatible constructor that defaults the data scope to
     * {@link DataScope#ALL} when not explicitly provided.
     */
    public AssignPermissionRequest(Long roleId, Long permissionId, String assignedBy) {
        this(roleId, permissionId, assignedBy, DataScope.ALL);
    }
}
