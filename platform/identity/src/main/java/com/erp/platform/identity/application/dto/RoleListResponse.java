package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.RoleType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for role list data (summary view).
 *
 * @since 1.0.0
 */
public record RoleListResponse(
    Long id,
    UUID roleId,
    String roleCode,
    String roleName,
    String description,
    RoleType roleType,
    boolean isSystemRole,
    boolean isActive,
    LocalDateTime createdAt
) {
}
