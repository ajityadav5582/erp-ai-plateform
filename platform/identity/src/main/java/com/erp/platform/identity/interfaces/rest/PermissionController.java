package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.PermissionService;
import com.erp.platform.identity.application.dto.CreatePermissionRequest;
import com.erp.platform.identity.application.dto.PermissionListResponse;
import com.erp.platform.identity.application.dto.PermissionResponse;
import com.erp.platform.identity.application.dto.UpdatePermissionRequest;
import com.erp.platform.identity.domain.Action;
import com.erp.platform.identity.domain.PermissionStatus;
import com.erp.platform.identity.domain.Resource;
import com.erp.platform.identity.domain.exception.CannotDeletePermissionException;
import com.erp.platform.identity.domain.exception.DuplicatePermissionCodeException;
import com.erp.platform.identity.domain.exception.PermissionNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * REST controller for permission management.
 *
 * <p>Exposes a standard REST API for permission CRUD operations, following the
 * platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/permissions}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/permissions")
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
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            UriComponentsBuilder uriBuilder) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/permissions/{id}").buildAndExpand(response.permissionId()).toUri())
                .body(response);
    }

    /**
     * Gets a permission by its business identifier.
     *
     * @param permissionId the permission UUID
     * @return 200 OK with the permission
     */
    @GetMapping("/{permissionId}")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable UUID permissionId) {
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
    public ResponseEntity<PermissionResponse> getPermissionByCode(@RequestParam String permissionCode) {
        PermissionResponse response = permissionService.getPermissionByCode(permissionCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all permissions with pagination and optional filtering.
     *
     * @param resource optional resource filter
     * @param action optional action filter
     * @param status optional status filter
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of permissions
     */
    @GetMapping
    public ResponseEntity<PermissionListResponse> listPermissions(
            @RequestParam(required = false) Resource resource,
            @RequestParam(required = false) Action action,
            @RequestParam(required = false) PermissionStatus status,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissions(resource, action, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists permissions by resource with pagination.
     *
     * @param resource the resource to filter by
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of permissions
     */
    @GetMapping("/by-resource")
    public ResponseEntity<PermissionListResponse> listPermissionsByResource(
            @RequestParam Resource resource,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissionsByResource(resource, pageable);
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
    public ResponseEntity<PermissionListResponse> listPermissionsByStatus(
            @RequestParam PermissionStatus status,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissionsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists permissions by action with pagination.
     *
     * @param action the action to filter by
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of permissions
     */
    @GetMapping("/by-action")
    public ResponseEntity<PermissionListResponse> listPermissionsByAction(
            @RequestParam Action action,
            Pageable pageable) {
        PermissionListResponse response = permissionService.listPermissionsByAction(action, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a permission.
     *
     * @param permissionId the permission UUID
     * @param request the update request
     * @return 200 OK with the updated permission
     */
    @PutMapping("/{permissionId}")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable UUID permissionId,
            @Valid @RequestBody UpdatePermissionRequest request) {
        PermissionResponse response = permissionService.updatePermission(permissionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a permission.
     *
     * @param permissionId the permission UUID
     * @param request the partial update request
     * @return 200 OK with the updated permission
     */
    @PatchMapping("/{permissionId}")
    public ResponseEntity<PermissionResponse> patchPermission(
            @PathVariable UUID permissionId,
            @RequestBody UpdatePermissionRequest request) {
        PermissionResponse response = permissionService.updatePermission(permissionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a permission (soft delete by deactivation).
     *
     * @param permissionId the permission UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }
}
