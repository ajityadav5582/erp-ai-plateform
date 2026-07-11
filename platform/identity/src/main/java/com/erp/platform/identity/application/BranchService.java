package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.BranchListResponse;
import com.erp.platform.identity.application.dto.BranchResponse;
import com.erp.platform.identity.application.dto.CreateBranchRequest;
import com.erp.platform.identity.application.dto.UpdateBranchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for branch management operations.
 *
 * @since 1.0.0
 */
public interface BranchService {

    /**
     * Create a new branch.
     *
     * @param tenantId the tenant ID
     * @param request the create branch request
     * @return the created branch response
     */
    BranchResponse createBranch(Long tenantId, CreateBranchRequest request);

    /**
     * Get a branch by its business identifier.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch UUID
     * @return the branch response
     */
    BranchResponse getBranchById(Long tenantId, UUID branchId);

    /**
     * Get a branch by its unique code within the tenant.
     *
     * @param tenantId the tenant ID
     * @param branchCode the branch code
     * @return the branch response
     */
    BranchResponse getBranchByCode(Long tenantId, String branchCode);

    /**
     * List all branches for a tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination parameters
     * @return page of branch list responses
     */
    Page<BranchListResponse> listBranches(Long tenantId, Pageable pageable);

    /**
     * Update an existing branch.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch UUID
     * @param request the update branch request
     * @return the updated branch response
     */
    BranchResponse updateBranch(Long tenantId, UUID branchId, UpdateBranchRequest request);

    /**
     * Activate a branch (INACTIVE → ACTIVE).
     *
     * @param tenantId the tenant ID
     * @param branchId the branch UUID
     * @return the activated branch response
     */
    BranchResponse activateBranch(Long tenantId, UUID branchId);

    /**
     * Deactivate a branch (ACTIVE → INACTIVE).
     *
     * @param tenantId the tenant ID
     * @param branchId the branch UUID
     * @return the deactivated branch response
     */
    BranchResponse deactivateBranch(Long tenantId, UUID branchId);

    /**
     * Delete a branch.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch UUID
     */
    void deleteBranch(Long tenantId, UUID branchId);
}
