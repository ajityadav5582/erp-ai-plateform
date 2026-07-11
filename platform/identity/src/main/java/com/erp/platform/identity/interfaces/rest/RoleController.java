package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.identity.application.RoleService;
import com.erp.platform.identity.application.dto.CreateRoleRequest;
import com.erp.platform.identity.application.dto.RoleListResponse;
import com.erp.platform.identity.application.dto.RoleResponse;
import com.erp.platform.identity.application.dto.UpdateRoleRequest;
import com.erp.platform.identity.domain.exception.CannotDeleteRoleException;
import com.erp.platform.identity.domain.exception.DuplicateRoleCodeException;
import com.erp.platform.identity.domain.exception.RoleNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * REST controller for role management.
 *
 * <p>Exposes a standard REST API for role CRUD operations, following the
 * platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/roles}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 *   <li>Tenant isolation via {@link TenantContext}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * Creates a new custom role.
     *
     * @param request the role creation request
     * @return 201 Created with the created role and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = getTenantId();
        RoleResponse response = roleService.createRole(tenantId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/roles/{id}").buildAndExpand(response.roleId()).toUri())
                .body(response);
    }

    /**
     * Gets a role by its business identifier.
     *
     * @param roleId the role UUID
     * @return 200 OK with the role
     */
    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID roleId) {
        RoleResponse response = roleService.getRoleById(roleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a role by its code.
     *
     * @param roleCode the role code
     * @return 200 OK with the role
     */
    @GetMapping("/by-code")
    public ResponseEntity<RoleResponse> getRoleByCode(@RequestParam String roleCode) {
        RoleResponse response = roleService.getRoleByCode(roleCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all roles with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of roles
     */
    @GetMapping
    public ResponseEntity<Page<RoleListResponse>> listRoles(Pageable pageable) {
        Page<RoleListResponse> page = roleService.listRoles(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lists roles by tenant with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of roles
     */
    @GetMapping("/by-tenant")
    public ResponseEntity<Page<RoleListResponse>> listRolesByTenant(Pageable pageable) {
        Long tenantId = getTenantId();
        Page<RoleListResponse> page = roleService.listRolesByTenant(tenantId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Fully updates a role.
     *
     * @param roleId the role UUID
     * @param request the update request
     * @return 200 OK with the updated role
     */
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        RoleResponse response = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a role.
     *
     * @param roleId the role UUID
     * @param request the partial update request
     * @return 200 OK with the updated role
     */
    @PatchMapping("/{roleId}")
    public ResponseEntity<RoleResponse> patchRole(
            @PathVariable UUID roleId,
            @RequestBody UpdateRoleRequest request) {
        RoleResponse response = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a role (soft delete by deactivation).
     *
     * @param roleId the role UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extracts the tenant ID from the current request context.
     *
     * @return the tenant ID as a {@link Long}
     * @throws IllegalStateException if the tenant context is not set
     */
    private Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant context is not set");
        }
        return Long.parseLong(tenantId);
    }
}
