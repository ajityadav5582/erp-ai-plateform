package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.BranchListResponse;
import com.erp.platform.identity.application.dto.BranchResponse;
import com.erp.platform.identity.application.dto.CreateBranchRequest;
import com.erp.platform.identity.application.dto.UpdateBranchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for branch management operations.
 *
 * <p>Every operation is scoped to both the tenant and the active company. A
 * tenant may own several companies, each with its own branch network, so a
 * branch belonging to another company of the same tenant must never be visible.
 *
 * @since 1.0.0
 */
public interface BranchService {

    /**
     * Create a new branch for the given company.
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param request the create branch request
     * @return the created branch response
     */
    BranchResponse createBranch(Long tenantId, Long companyId, CreateBranchRequest request);

    /**
     * Get a branch by its identifier within the active company.
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param branchId the branch ID
     * @return the branch response
     */
    BranchResponse getBranchById(Long tenantId, Long companyId, Long branchId);

    /**
     * Get a branch by its unique code within the active company.
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param branchCode the branch code
     * @return the branch response
     */
    BranchResponse getBranchByCode(Long tenantId, Long companyId, String branchCode);

    /**
     * List all branches of the active company with pagination.
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param pageable the pagination parameters
     * @return page of branch list responses
     */
    Page<BranchListResponse> listBranches(Long tenantId, Long companyId, Pageable pageable);

    /**
     * Update an existing branch of the active company.
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param branchId the branch ID
     * @param request the update branch request
     * @return the updated branch response
     */
    BranchResponse updateBranch(Long tenantId, Long companyId, Long branchId, UpdateBranchRequest request);

    /**
     * Activate a branch (INACTIVE &rarr; ACTIVE).
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param branchId the branch ID
     * @return the activated branch response
     */
    BranchResponse activateBranch(Long tenantId, Long companyId, Long branchId);

    /**
     * Deactivate a branch (ACTIVE &rarr; INACTIVE).
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param branchId the branch ID
     * @return the deactivated branch response
     */
    BranchResponse deactivateBranch(Long tenantId, Long companyId, Long branchId);

    /**
     * Delete a branch.
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param branchId the branch ID
     */
    void deleteBranch(Long tenantId, Long companyId, Long branchId);
}
