package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.AssignPermissionRequest;
import com.erp.platform.identity.application.dto.RemovePermissionRequest;
import com.erp.platform.identity.application.dto.RolePermissionListResponse;
import com.erp.platform.identity.application.dto.RolePermissionResponse;

import java.util.UUID;

/**
 * Service interface for managing role-permission assignments.
 *
 * <p>Provides operations for assigning and removing permissions from roles,
 * as well as querying role permissions.
 *
 * @since 1.0.0
 */
public interface RolePermissionService {

    /**
     * Assigns a permission to a role.
     *
     * <p>Creates a new role-permission association with audit metadata.
     *
     * @param request the assignment request
     * @return the created role permission response
     */
    RolePermissionResponse assignPermission(AssignPermissionRequest request);

    /**
     * Removes a permission from a role.
     *
     * <p>Deletes the role-permission association.
     *
     * @param request the removal request
     */
    void removePermission(RemovePermissionRequest request);

    /**
     * Finds all permissions assigned to a role.
     *
     * @param roleId the role ID
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated list of role permissions
     */
    RolePermissionListResponse findPermissionsByRoleId(Long roleId, int page, int size);

    /**
     * Checks if a role has a specific permission assigned.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @return true if the role has the permission, false otherwise
     */
    boolean hasPermission(Long roleId, Long permissionId);

    /**
     * Finds a role permission assignment by its business identifier.
     *
     * @param rolePermissionId the role permission assignment UUID
     * @return the role permission response, or null if not found
     */
    RolePermissionResponse findByRolePermissionId(UUID rolePermissionId);
}
