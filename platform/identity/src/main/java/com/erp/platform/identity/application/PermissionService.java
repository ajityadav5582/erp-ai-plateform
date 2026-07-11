package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreatePermissionRequest;
import com.erp.platform.identity.application.dto.PermissionListResponse;
import com.erp.platform.identity.application.dto.PermissionResponse;
import com.erp.platform.identity.application.dto.UpdatePermissionRequest;
import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Resource;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Permission aggregate operations.
 *
 * @since 1.0.0
 */
public interface PermissionService {

    /**
     * Create a new permission.
     *
     * @param request the create permission request
     * @return the created permission response
     */
    PermissionResponse createPermission(CreatePermissionRequest request);

    /**
     * Update an existing permission.
     *
     * @param permissionId the permission ID
     * @param request the update permission request
     * @return the updated permission response
     */
    PermissionResponse updatePermission(UUID permissionId, UpdatePermissionRequest request);

    /**
     * Delete a permission (soft delete via deactivation).
     *
     * @param permissionId the permission ID
     */
    void deletePermission(UUID permissionId);

    /**
     * Get a permission by ID.
     *
     * @param permissionId the permission ID
     * @return the permission response
     */
    PermissionResponse getPermissionById(UUID permissionId);

    /**
     * Get a permission by permission code.
     *
     * @param permissionCode the permission code
     * @return the permission response
     */
    PermissionResponse getPermissionByCode(String permissionCode);

    /**
     * List all permissions with pagination and optional filtering.
     *
     * @param resource optional resource filter
     * @param action optional action filter
     * @param status optional status filter
     * @param pageable the pagination information
     * @return the paginated permission list response
     */
    PermissionListResponse listPermissions(Resource resource, Action action, PermissionStatus status, Pageable pageable);

    /**
     * List all permissions by resource with pagination.
     *
     * @param resource the resource to filter by
     * @param pageable the pagination information
     * @return the paginated permission list response
     */
    PermissionListResponse listPermissionsByResource(Resource resource, Pageable pageable);

    /**
     * List all permissions by status with pagination.
     *
     * @param status the status to filter by
     * @param pageable the pagination information
     * @return the paginated permission list response
     */
    PermissionListResponse listPermissionsByStatus(PermissionStatus status, Pageable pageable);

    /**
     * List all permissions by action with pagination.
     *
     * @param action the action to filter by
     * @param pageable the pagination information
     * @return the paginated permission list response
     */
    PermissionListResponse listPermissionsByAction(Action action, Pageable pageable);

    /**
     * Check if a permission exists by permission code.
     *
     * @param permissionCode the permission code
     * @return true if the permission exists, false otherwise
     */
    boolean existsByPermissionCode(String permissionCode);

    /**
     * Check if a permission exists by permission ID.
     *
     * @param permissionId the permission ID
     * @return true if the permission exists, false otherwise
     */
    boolean existsByPermissionId(UUID permissionId);
}
