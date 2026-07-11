package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.identity.application.BranchService;
import com.erp.platform.identity.application.dto.BranchListResponse;
import com.erp.platform.identity.application.dto.BranchResponse;
import com.erp.platform.identity.application.dto.CreateBranchRequest;
import com.erp.platform.identity.application.dto.UpdateBranchRequest;
import com.erp.platform.identity.domain.exception.BranchNotFoundException;
import com.erp.platform.identity.domain.exception.BranchOperationException;
import com.erp.platform.identity.domain.exception.DuplicateBranchCodeException;
import com.erp.platform.identity.domain.exception.DuplicateBranchNameException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * REST controller for branch management.
 *
 * <p>Exposes a standard REST API for branch CRUD operations and lifecycle state
 * transitions, following the platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/branches}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 *   <li>Tenant isolation via {@link TenantContext}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    /**
     * Creates a new branch.
     *
     * @param request the branch creation request
     * @return 201 Created with the created branch and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = getTenantId();
        BranchResponse response = branchService.createBranch(tenantId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/branches/{id}").buildAndExpand(response.branchId()).toUri())
                .body(response);
    }

    /**
     * Lists all branches for the current tenant with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of branches
     */
    @GetMapping
    public ResponseEntity<Page<BranchListResponse>> listBranches(Pageable pageable) {
        Long tenantId = getTenantId();
        Page<BranchListResponse> page = branchService.listBranches(tenantId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Gets a branch by its business identifier.
     *
     * @param branchId the branch UUID
     * @return 200 OK with the branch
     */
    @GetMapping("/{branchId}")
    public ResponseEntity<BranchResponse> getBranchById(@PathVariable UUID branchId) {
        Long tenantId = getTenantId();
        BranchResponse response = branchService.getBranchById(tenantId, branchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a branch by its unique code within the tenant.
     *
     * @param code the branch code
     * @return 200 OK with the branch
     */
    @GetMapping("/by-code")
    public ResponseEntity<BranchResponse> getBranchByCode(@RequestParam String code) {
        Long tenantId = getTenantId();
        BranchResponse response = branchService.getBranchByCode(tenantId, code);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a branch.
     *
     * @param branchId the branch UUID
     * @param request the update request
     * @return 200 OK with the updated branch
     */
    @PutMapping("/{branchId}")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateBranchRequest request) {
        Long tenantId = getTenantId();
        BranchResponse response = branchService.updateBranch(tenantId, branchId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a branch.
     *
     * @param branchId the branch UUID
     * @param request the partial update request
     * @return 200 OK with the updated branch
     */
    @PatchMapping("/{branchId}")
    public ResponseEntity<BranchResponse> patchBranch(
            @PathVariable UUID branchId,
            @RequestBody UpdateBranchRequest request) {
        Long tenantId = getTenantId();
        BranchResponse response = branchService.updateBranch(tenantId, branchId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a branch.
     *
     * @param branchId the branch UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{branchId}")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID branchId) {
        Long tenantId = getTenantId();
        branchService.deleteBranch(tenantId, branchId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a branch (INACTIVE → ACTIVE).
     *
     * @param branchId the branch UUID
     * @return 200 OK with the activated branch
     */
    @PostMapping("/{branchId}/activate")
    public ResponseEntity<BranchResponse> activateBranch(@PathVariable UUID branchId) {
        Long tenantId = getTenantId();
        BranchResponse response = branchService.activateBranch(tenantId, branchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a branch (ACTIVE → INACTIVE).
     *
     * @param branchId the branch UUID
     * @return 200 OK with the deactivated branch
     */
    @PostMapping("/{branchId}/deactivate")
    public ResponseEntity<BranchResponse> deactivateBranch(@PathVariable UUID branchId) {
        Long tenantId = getTenantId();
        BranchResponse response = branchService.deactivateBranch(tenantId, branchId);
        return ResponseEntity.ok(response);
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
