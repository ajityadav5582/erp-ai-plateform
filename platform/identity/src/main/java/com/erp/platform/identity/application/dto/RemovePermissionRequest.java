package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for removing a permission from a role.
 *
 * @since 1.0.0
 */
public record RemovePermissionRequest(
        @NotNull(message = "Role ID is required")
        Long roleId,

        @NotNull(message = "Permission ID is required")
        Long permissionId,

        String revokedBy,

        String revokeReason
) {
}
