package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.BranchService;
import com.erp.platform.identity.application.CurrentTenantProvider;
import com.erp.platform.identity.application.dto.BranchListResponse;
import com.erp.platform.identity.application.dto.BranchResponse;
import com.erp.platform.identity.application.dto.CreateBranchRequest;
import com.erp.platform.identity.application.dto.UpdateBranchRequest;
import com.erp.platform.identity.application.security.RequirePermission;
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

/**
 * REST controller for branch management.
 *
 * <p>Exposes a standard REST API for branch CRUD operations and lifecycle state
 * transitions, following the platform API standards:
 * <ul>
 *   <li>Versioned base path {@code /api/v1/identity/branches}</li>
 *   <li>Proper HTTP methods (GET, POST, PUT, PATCH, DELETE)</li>
 *   <li>Appropriate status codes (201 + Location on create, 204 on delete)</li>
 *   <li>Pagination and filtering on collection endpoints</li>
 *   <li>Tenant isolation via {@link TenantContext}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/identity/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final CurrentTenantProvider currentTenantProvider;

    /**
     * Creates a new branch.
     *
     * @param request the branch creation request
     * @return 201 Created with the created branch and a {@code Location} header
     */
    @PostMapping
    @RequirePermission("BRANCH_CREATE")
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        BranchResponse response = branchService.createBranch(tenantId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/identity/branches/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Lists all branches for the current tenant with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return 200 OK with a page of branches
     */
    @GetMapping
    @RequirePermission("BRANCH_READ")
    public ResponseEntity<Page<BranchListResponse>> listBranches(Pageable pageable) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        Page<BranchListResponse> page = branchService.listBranches(tenantId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Gets a branch by its business identifier.
     *
     * @param branchId the branch ID
     * @return 200 OK with the branch
     */
    @GetMapping("/{branchId}")
    @RequirePermission("BRANCH_READ")
    public ResponseEntity<BranchResponse> getBranchById(@PathVariable Long branchId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
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
    @RequirePermission("BRANCH_READ")
    public ResponseEntity<BranchResponse> getBranchByCode(@RequestParam String code) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        BranchResponse response = branchService.getBranchByCode(tenantId, code);
        return ResponseEntity.ok(response);
    }

    /**
     * Fully updates a branch.
     *
     * @param branchId the branch ID
     * @param request the update request
     * @return 200 OK with the updated branch
     */
    @PutMapping("/{branchId}")
    @RequirePermission("BRANCH_UPDATE")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Long branchId,
            @Valid @RequestBody UpdateBranchRequest request) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        BranchResponse response = branchService.updateBranch(tenantId, branchId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a branch.
     *
     * @param branchId the branch ID
     * @param request the partial update request
     * @return 200 OK with the updated branch
     */
    @PatchMapping("/{branchId}")
    @RequirePermission("BRANCH_UPDATE")
    public ResponseEntity<BranchResponse> patchBranch(
            @PathVariable Long branchId,
            @RequestBody UpdateBranchRequest request) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        BranchResponse response = branchService.updateBranch(tenantId, branchId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a branch.
     *
     * @param branchId the branch ID
     * @return 204 No Content
     */
    @DeleteMapping("/{branchId}")
    @RequirePermission("BRANCH_DELETE")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long branchId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        branchService.deleteBranch(tenantId, branchId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a branch (INACTIVE → ACTIVE).
     *
     * @param branchId the branch ID
     * @return 200 OK with the activated branch
     */
    @PostMapping("/{branchId}/activate")
    @RequirePermission("BRANCH_UPDATE")
    public ResponseEntity<BranchResponse> activateBranch(@PathVariable Long branchId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        BranchResponse response = branchService.activateBranch(tenantId, branchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a branch (ACTIVE → INACTIVE).
     *
     * @param branchId the branch ID
     * @return 200 OK with the deactivated branch
     */
    @PostMapping("/{branchId}/deactivate")
    @RequirePermission("BRANCH_UPDATE")
    public ResponseEntity<BranchResponse> deactivateBranch(@PathVariable Long branchId) {
        Long tenantId = currentTenantProvider.getCurrentTenantId();
        BranchResponse response = branchService.deactivateBranch(tenantId, branchId);
        return ResponseEntity.ok(response);
    }

}
