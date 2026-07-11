package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for removing a role from a user.
 *
 * @since 1.0.0
 */
public record RemoveRoleRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Role ID is required")
        Long roleId,

        String revokedBy,

        String revokeReason
) {
}
