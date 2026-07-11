package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for assigning a role to a user.
 *
 * @since 1.0.0
 */
public record AssignRoleRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Role ID is required")
        Long roleId,

        LocalDateTime expiresAt,

        Boolean isPrimaryRole,

        String assignedBy
) {
}
