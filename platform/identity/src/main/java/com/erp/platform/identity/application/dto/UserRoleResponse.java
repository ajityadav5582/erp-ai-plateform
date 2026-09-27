package com.erp.platform.identity.application.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for UserRole assignment.
 *
 * @since 1.0.0
 */
public record UserRoleResponse(
        Long id,
        Long userId,
        Long roleId,
        String roleName,
        String roleCode,
        String assignedBy,
        LocalDateTime assignedAt,
        LocalDateTime expiresAt,
        Boolean isPrimaryRole,
        boolean active
) {
}
