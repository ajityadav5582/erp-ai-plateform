package com.erp.platform.identity.application.dto;

import com.erp.platform.common.enums.Status;
import com.erp.platform.identity.domain.RoleType;

/**
 * Response DTO for role list data (summary view).
 *
 * @since 1.0.0
 */
public record RoleListResponse(
    Long id,
    String roleCode,
    String roleName,
    String description,
    RoleType roleType,
    Status status,
    Boolean isEditable,
    Boolean isDeletable,
    boolean isSystemRole,
    boolean isActive
) {
}
