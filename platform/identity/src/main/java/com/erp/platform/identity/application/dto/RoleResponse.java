package com.erp.platform.identity.application.dto;

import com.erp.platform.common.enums.Status;
import com.erp.platform.identity.domain.RoleType;
import java.util.Set;

/**
 * Response DTO for role data.
 *
 * @since 1.0.0
 */
public record RoleResponse(
    Long id,
    Long tenantId,
    String roleCode,
    String roleName,
    String description,
    RoleType roleType,
    Status status,
    Boolean isEditable,
    Boolean isDeletable,
    boolean isSystemRole,
    boolean isActive,
    Set<Long> permissionIds
) {
    public RoleResponse(Long id, Long tenantId, String roleCode, String roleName, String description,
                        RoleType roleType, Status status, Boolean isEditable, Boolean isDeletable,
                        boolean isSystemRole, boolean isActive) {
        this(id, tenantId, roleCode, roleName, description, roleType, status, isEditable, isDeletable, isSystemRole, isActive, Set.of());
    }
}
