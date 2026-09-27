package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreatePermissionRequest;
import com.erp.platform.identity.application.dto.PermissionListResponse;
import com.erp.platform.identity.application.dto.PermissionResponse;
import com.erp.platform.identity.application.dto.UpdatePermissionRequest;
import com.erp.platform.identity.domain.PermissionStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Permission aggregate operations.
 *
 * @since 1.0.0
 */
public interface PermissionService {

    /**
     * Creates a new permission.
     *
     * @param request the creation request
     * @return the created permission response
     */
    PermissionResponse createPermission(CreatePermissionRequest request);

    /**
     * Updates an existing permission.
     *
     * @param permissionId the permission ID
     * @param request the update request
     * @return the updated permission response
     */
    PermissionResponse updatePermission(Long permissionId, UpdatePermissionRequest request);

    /**
     * Deletes (deactivates) a permission.
     *
     * @param permissionId the permission ID
     */
    void deletePermission(Long permissionId);

    /**
     * Gets a permission by its ID.
     *
     * @param permissionId the permission ID
     * @return the permission response
     */
    PermissionResponse getPermissionById(Long permissionId);

    /**
     * Gets a permission by its code.
     *
     * @param permissionCode the permission code
     * @return the permission response
     */
    PermissionResponse getPermissionByCode(String permissionCode);

    /**
     * Lists permissions with optional filters.
     *
     * @param resourceCode the resource code (optional)
     * @param actionCode the action code (optional)
     * @param status the permission status (optional)
     * @param pageable pagination info
     * @return the paginated permission list
     */
    PermissionListResponse listPermissions(String resourceCode, String actionCode, PermissionStatus status, Pageable pageable);

    /**
     * Lists permissions filtered by resource.
     *
     * @param resourceCode the resource code
     * @param pageable pagination info
     * @return the paginated permission list
     */
    PermissionListResponse listPermissionsByResource(String resourceCode, Pageable pageable);

    /**
     * Lists permissions filtered by status.
     *
     * @param status the permission status
     * @param pageable pagination info
     * @return the paginated permission list
     */
    PermissionListResponse listPermissionsByStatus(PermissionStatus status, Pageable pageable);

    /**
     * Lists permissions filtered by action.
     *
     * @param actionCode the action code
     * @param pageable pagination info
     * @return the paginated permission list
     */
    PermissionListResponse listPermissionsByAction(String actionCode, Pageable pageable);

    /**
     * Checks if a permission code exists.
     *
     * @param permissionCode the permission code
     * @return true if exists
     */
    boolean existsByPermissionCode(String permissionCode);

    /**
     * Checks if a permission ID exists.
     *
     * @param permissionId the permission ID
     * @return true if exists
     */
    boolean existsByPermissionId(Long permissionId);
}
