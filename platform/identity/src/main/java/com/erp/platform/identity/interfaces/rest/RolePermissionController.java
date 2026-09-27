package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.RolePermissionService;
import com.erp.platform.identity.application.dto.AssignPermissionRequest;
import com.erp.platform.identity.application.dto.RemovePermissionRequest;
import com.erp.platform.identity.application.dto.RolePermissionListResponse;
import com.erp.platform.identity.application.dto.RolePermissionResponse;
import com.erp.platform.identity.application.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for role-permission assignment management.
 *
 * <p>Exposes a standard REST API for managing the many-to-many relationship
 * between roles and permissions, following the platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/role-permissions}</li>
 *   <li>Proper HTTP methods (POST, DELETE, GET)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination on collection endpoints</li>
 *   <li>Tenant isolation is enforced by the application service</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    /**
     * Assigns a permission to a role.
     *
     * @param request the permission assignment request
     * @return 201 Created with the created assignment and a {@code Location} header
     */
    @PostMapping
    @RequirePermission("ROLE_PERMISSION_CREATE")
    public ResponseEntity<RolePermissionResponse> assignPermission(
            @Valid @RequestBody AssignPermissionRequest request,
            UriComponentsBuilder uriBuilder) {
        RolePermissionResponse response = rolePermissionService.assignPermission(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/role-permissions/{id}")
                        .buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Removes a permission from a role.
     *
     * @param request the permission removal request
     * @return 204 No Content
     */
    @DeleteMapping
    @RequirePermission("ROLE_PERMISSION_DELETE")
    public ResponseEntity<Void> removePermission(@Valid @RequestBody RemovePermissionRequest request) {
        rolePermissionService.removePermission(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets a role permission assignment by its database identifier.
     *
     * @param id the role permission assignment ID
     * @return 200 OK with the assignment, or 404 if not found
     */
    @GetMapping("/{id}")
    @RequirePermission("ROLE_PERMISSION_READ")
    public ResponseEntity<RolePermissionResponse> getRolePermissionById(@PathVariable Long id) {
        RolePermissionResponse response = rolePermissionService.findById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all permissions assigned to a role with pagination.
     *
     * @param roleId the role ID
     * @param page the page number (0-based)
     * @param size the page size
     * @return 200 OK with a page of role permissions
     */
    @GetMapping("/role/{roleId}")
    @RequirePermission("ROLE_PERMISSION_READ")
    public ResponseEntity<RolePermissionListResponse> listPermissionsByRole(
            @PathVariable Long roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        RolePermissionListResponse response = rolePermissionService.findPermissionsByRoleId(roleId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a role has a specific permission assigned.
     *
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @return 200 OK with true/false
     */
    @GetMapping("/check")
    @RequirePermission("ROLE_PERMISSION_READ")
    public ResponseEntity<Boolean> hasPermission(
            @RequestParam Long roleId,
            @RequestParam Long permissionId) {
        boolean hasPermission = rolePermissionService.hasPermission(roleId, permissionId);
        return ResponseEntity.ok(hasPermission);
    }
}
