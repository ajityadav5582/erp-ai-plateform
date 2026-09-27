package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateRoleRequest;
import com.erp.platform.identity.application.dto.RoleListResponse;
import com.erp.platform.identity.application.dto.RoleResponse;
import com.erp.platform.identity.domain.Role;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Mapper for converting between Role entity and DTOs.
 *
 * @since 1.0.0
 */
@Component
public class RoleMapper {

    /**
     * Convert CreateRoleRequest to Role entity.
     *
     * @param request the create role request
     * @param tenantId the tenant ID
     * @return the Role entity
     */
    public Role toEntity(CreateRoleRequest request, Long tenantId) {
        return Role.createCustom(
            tenantId,
            request.roleCode(),
            request.roleName(),
            request.description()
        );
    }

    /**
     * Convert Role entity to RoleResponse.
     *
     * @param role the role entity
     * @return the RoleResponse
     */
    public RoleResponse toResponse(Role role) {
        return toResponse(role, Set.of());
    }

    /**
     * Convert Role entity and permission IDs to RoleResponse.
     *
     * @param role the role entity
     * @param permissionIds assigned permission IDs
     * @return the RoleResponse
     */
    public RoleResponse toResponse(Role role, Set<Long> permissionIds) {
        return new RoleResponse(
            role.getId(),
            role.getTenantId(),
            role.getRoleCode(),
            role.getRoleName(),
            role.getDescription(),
            role.getRoleType(),
            role.getStatus(),
            role.canBeModified(),
            role.canBeDeactivated(),
            role.isSystemRole(),
            role.isActive(),
            permissionIds != null ? permissionIds : Set.of()
        );
    }

    /**
     * Convert Role entity to RoleListResponse.
     *
     * @param role the role entity
     * @return the RoleListResponse
     */
    public RoleListResponse toListResponse(Role role) {
        return new RoleListResponse(
            role.getId(),
            role.getRoleCode(),
            role.getRoleName(),
            role.getDescription(),
            role.getRoleType(),
            role.getStatus(),
            role.canBeModified(),
            role.canBeDeactivated(),
            role.isSystemRole(),
            role.isActive()
        );
    }
}
