package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for UserRole assignment.
 *
 * @since 1.0.0
 */
public record UserRoleResponse(
        Long id,
        UUID userRoleId,
        Long userId,
        Long roleId,
        String roleName,
        String roleCode,
        Long tenantId,
        String assignedBy,
        LocalDateTime assignedAt,
        LocalDateTime expiresAt,
        Boolean isPrimaryRole,
        LocalDateTime revokedAt,
        String revokedBy,
        String revokeReason,
        Integer version
) {
}
