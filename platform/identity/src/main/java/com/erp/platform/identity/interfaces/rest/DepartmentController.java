package com.erp.platform.identity.interfaces.rest;


import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.identity.application.DepartmentService;
import com.erp.platform.identity.application.dto.CreateDepartmentRequest;
import com.erp.platform.identity.application.dto.DepartmentListResponse;
import com.erp.platform.identity.application.dto.DepartmentResponse;
import com.erp.platform.identity.application.dto.UpdateDepartmentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for department management.
 *
 * <p>Exposes a standard REST API for department CRUD operations, hierarchical
 * (parent/child) navigation, and lifecycle state transitions, following the
 * platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/departments}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 *   <li>Tenant isolation via {@link TenantContext}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Creates a new department.
     *
     * @param request the department creation request
     * @return 201 Created with the created department and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.createDepartment(tenantId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/departments/{id}").buildAndExpand(response.departmentId()).toUri())
                .body(response);
    }

    /**
     * Lists all departments for the current tenant with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of departments
     */
    @GetMapping
    public ResponseEntity<Page<DepartmentListResponse>> listDepartments(Pageable pageable) {
        Long tenantId = getTenantId();
        Page<DepartmentListResponse> page = departmentService.listDepartments(tenantId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Gets a department by its business identifier.
     *
     * @param departmentId the department UUID
     * @return 200 OK with the department
     */
    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable UUID departmentId) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.getDepartmentById(tenantId, departmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a department by its unique code within a branch.
     *
     * @param branchId the branch ID
     * @param departmentCode the department code
     * @return 200 OK with the department
     */
    @GetMapping("/by-code")
    public ResponseEntity<DepartmentResponse> getDepartmentByCode(
            @RequestParam Long branchId,
            @RequestParam String departmentCode) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.getDepartmentByCode(tenantId, branchId, departmentCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all root departments (departments without a parent) for a branch.
     *
     * @param branchId the branch ID
     * @return 200 OK with the list of root departments
     */
    @GetMapping("/roots")
    public ResponseEntity<List<DepartmentListResponse>> listRootDepartments(@RequestParam Long branchId) {
        Long tenantId = getTenantId();
        List<DepartmentListResponse> response = departmentService.listRootDepartments(tenantId, branchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all child departments of a given parent department.
     *
     * @param parentDepartmentId the parent department ID
     * @return 200 OK with the list of child departments
     */
    @GetMapping("/children")
    public ResponseEntity<List<DepartmentListResponse>> listChildDepartments(
            @RequestParam Long parentDepartmentId) {
        Long tenantId = getTenantId();
        List<DepartmentListResponse> response = departmentService.listChildDepartments(tenantId, parentDepartmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a department.
     *
     * @param departmentId the department UUID
     * @param request the update request
     * @return 200 OK with the updated department
     */
    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable UUID departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.updateDepartment(tenantId, departmentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a department.
     *
     * @param departmentId the department UUID
     * @param request the partial update request
     * @return 200 OK with the updated department
     */
    @PatchMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> patchDepartment(
            @PathVariable UUID departmentId,
            @RequestBody UpdateDepartmentRequest request) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.updateDepartment(tenantId, departmentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Moves a department to a new parent (or to root when parent is null).
     *
     * @param departmentId the department UUID
     * @param newParentDepartmentId the new parent department ID, or null to make it a root department
     * @return 200 OK with the moved department
     */
    @PostMapping("/{departmentId}/move")
    public ResponseEntity<DepartmentResponse> moveDepartment(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) Long newParentDepartmentId) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.moveDepartment(tenantId, departmentId, newParentDepartmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Activates a department (INACTIVE → ACTIVE).
     *
     * @param departmentId the department UUID
     * @return 200 OK with the activated department
     */
    @PostMapping("/{departmentId}/activate")
    public ResponseEntity<DepartmentResponse> activateDepartment(@PathVariable UUID departmentId) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.activateDepartment(tenantId, departmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a department (ACTIVE → INACTIVE).
     *
     * @param departmentId the department UUID
     * @return 200 OK with the deactivated department
     */
    @PostMapping("/{departmentId}/deactivate")
    public ResponseEntity<DepartmentResponse> deactivateDepartment(@PathVariable UUID departmentId) {
        Long tenantId = getTenantId();
        DepartmentResponse response = departmentService.deactivateDepartment(tenantId, departmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a department.
     *
     * @param departmentId the department UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{departmentId}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID departmentId) {
        Long tenantId = getTenantId();
        departmentService.deleteDepartment(tenantId, departmentId);
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
