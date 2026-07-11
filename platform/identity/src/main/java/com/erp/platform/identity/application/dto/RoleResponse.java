package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.RoleType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for role data.
 *
 * @since 1.0.0
 */
public record RoleResponse(
    Long id,
    UUID roleId,
    Long tenantId,
    String roleCode,
    String roleName,
    String description,
    RoleType roleType,
    boolean isSystemRole,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy,
    Integer version
) {
}
