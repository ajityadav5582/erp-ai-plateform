package com.erp.platform.identity.application.mapper;

import com.erp.platform.identity.application.dto.CreateRoleRequest;
import com.erp.platform.identity.application.dto.RoleListResponse;
import com.erp.platform.identity.application.dto.RoleResponse;
import com.erp.platform.identity.domain.Role;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

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
     * @param tenantId the tenant ID (null for system roles)
     * @return the Role entity
     */
    public Role toEntity(CreateRoleRequest request, Long tenantId) {
        return Role.createCustom(
            UUID.randomUUID(),
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
        return new RoleResponse(
            role.getId(),
            role.getRoleId(),
            role.getTenantId(),
            role.getRoleCode(),
            role.getRoleName(),
            role.getDescription(),
            role.getRoleType(),
            role.isSystemRole(),
            role.isActive(),
            toLocalDateTime(role.getCreatedAt()),
            toLocalDateTime(role.getUpdatedAt()),
            role.getCreatedBy(),
            role.getUpdatedBy(),
            role.getVersion()
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
            role.getRoleId(),
            role.getRoleCode(),
            role.getRoleName(),
            role.getDescription(),
            role.getRoleType(),
            role.isSystemRole(),
            role.isActive(),
            toLocalDateTime(role.getCreatedAt())
        );
    }

    /**
     * Convert Instant to LocalDateTime.
     *
     * @param instant the instant to convert
     * @return the local date time
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
