package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.PermissionService;
import com.erp.platform.identity.application.dto.CreatePermissionRequest;
import com.erp.platform.identity.application.dto.PermissionListResponse;
import com.erp.platform.identity.application.dto.PermissionResponse;
import com.erp.platform.identity.application.dto.UpdatePermissionRequest;
import com.erp.platform.identity.application.security.RequirePermission;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.exception.CannotDeletePermissionException;
import com.erp.platform.identity.domain.exception.DuplicatePermissionCodeException;
import com.erp.platform.identity.domain.exception.PermissionNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for permission management.
 *
 * <p>Exposes a standard REST API for permission CRUD operations, following the
 * platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/identity/permissions}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/identity/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * Creates a new permission.
     *
     * @param request the permission creation request
     * @return 201 Created with the created permission and a {@code Location} header
     */
    @PostMapping
    @RequirePermission("PERMISSION_CREATE")
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            UriComponentsBuilder uriBuilder) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/identity/permissions/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Gets a permission by its ID.
     *
     * @param permissionId the permission ID
     * @return 200 OK with the permission
     */
    @GetMapping("/{permissionId}")
    @RequirePermission("PERMISSION_READ")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable Long permissionId) {
        PermissionResponse response = permissionService.getPermissionById(permissionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a permission by its code.
     *
     * @param permissionCode the permission code
     * @return 200 OK with the permission
     */
    @GetMapping("/by-code")
    @RequirePermission("PERMISSION_READ")
    public ResponseEntity<PermissionResponse> getPermissionByCode(@RequestParam String permissionCode) {
        PermissionResponse response = permissionService.getPermissionByCode(permissionCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all permissions with pagination and optional filtering.
     *
     * @param resourceCode optional resource code filter
     * @param actionCode optional action code filter
     * @param status optional status filter
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of permissions
     */
    @GetMapping
    @RequirePermission("PERMISSION_READ")
    public ResponseEntity<PermissionListResponse> listPermissions(
            @RequestParam(required = false) String resourceCode,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) PermissionStatus status,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissions(resourceCode, actionCode, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists permissions by resource with pagination.
     *
     * @param resourceCode the resource code to filter by
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of permissions
     */
    @GetMapping("/by-resource")
    @RequirePermission("PERMISSION_READ")
    public ResponseEntity<PermissionListResponse> listPermissionsByResource(
            @RequestParam String resourceCode,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissionsByResource(resourceCode, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists permissions by status with pagination.
     *
     * @param status the status to filter by
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of permissions
     */
    @GetMapping("/by-status")
    @RequirePermission("PERMISSION_READ")
    public ResponseEntity<PermissionListResponse> listPermissionsByStatus(
            @RequestParam PermissionStatus status,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissionsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists permissions by action with pagination.
     *
     * @param actionCode the action code to filter by
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of permissions
     */
    @GetMapping("/by-action")
    @RequirePermission("PERMISSION_READ")
    public ResponseEntity<PermissionListResponse> listPermissionsByAction(
            @RequestParam String actionCode,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissionsByAction(actionCode, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a permission.
     *
     * @param permissionId the permission ID
     * @param request the update request
     * @return 200 OK with the updated permission
     */
    @PutMapping("/{permissionId}")
    @RequirePermission("PERMISSION_UPDATE")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable Long permissionId,
            @Valid @RequestBody UpdatePermissionRequest request) {
        PermissionResponse response = permissionService.updatePermission(permissionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a permission.
     *
     * @param permissionId the permission ID
     * @param request the partial update request
     * @return 200 OK with the updated permission
     */
    @PatchMapping("/{permissionId}")
    @RequirePermission("PERMISSION_UPDATE")
    public ResponseEntity<PermissionResponse> patchPermission(
            @PathVariable Long permissionId,
            @RequestBody UpdatePermissionRequest request) {
        PermissionResponse response = permissionService.updatePermission(permissionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a permission (soft delete by deactivation).
     *
     * @param permissionId the permission ID
     * @return 204 No Content
     */
    @DeleteMapping("/{permissionId}")
    @RequirePermission("PERMISSION_DELETE")
    public ResponseEntity<Void> deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }
}
