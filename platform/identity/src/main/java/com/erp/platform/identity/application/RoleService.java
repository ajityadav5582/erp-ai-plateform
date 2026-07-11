package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateRoleRequest;
import com.erp.platform.identity.application.dto.RoleListResponse;
import com.erp.platform.identity.application.dto.RoleResponse;
import com.erp.platform.identity.application.dto.UpdateRoleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for role management operations.
 *
 * @since 1.0.0
 */
public interface RoleService {

    /**
     * Create a new custom role.
     *
     * @param tenantId the tenant ID (null for system roles)
     * @param request the create role request
     * @return the created role response
     */
    RoleResponse createRole(Long tenantId, CreateRoleRequest request);

    /**
     * Update an existing role.
     *
     * @param roleId the role UUID
     * @param request the update role request
     * @return the updated role response
     */
    RoleResponse updateRole(UUID roleId, UpdateRoleRequest request);

    /**
     * Get role by ID.
     *
     * @param roleId the role UUID
     * @return the role response
     */
    RoleResponse getRoleById(UUID roleId);

    /**
     * Get role by code.
     *
     * @param roleCode the role code
     * @return the role response
     */
    RoleResponse getRoleByCode(String roleCode);

    /**
     * List all roles with pagination.
     *
     * @param pageable the pagination parameters
     * @return page of role list responses
     */
    Page<RoleListResponse> listRoles(Pageable pageable);

    /**
     * List roles by tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination parameters
     * @return page of role list responses
     */
    Page<RoleListResponse> listRolesByTenant(Long tenantId, Pageable pageable);

    /**
     * Delete a role (soft delete by deactivation).
     *
     * @param roleId the role UUID
     */
    void deleteRole(UUID roleId);
}
