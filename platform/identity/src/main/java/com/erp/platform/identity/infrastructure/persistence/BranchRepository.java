package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Branch;
import com.erp.platform.identity.domain.BranchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Branch aggregate.
 *
 * <p>Provides data access methods for branch management operations.
 * All queries are tenant-scoped to ensure data isolation.
 *
 * @since 1.0.0
 */
public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Finds a branch by tenant ID and its identifier.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch identifier
     * @return the branch if found, empty otherwise
     */
    Optional<Branch> findByTenantIdAndId(Long tenantId, Long branchId);

    /**
     * Finds a branch scoped to the active company.
     *
     * @param tenantId the tenant ID
     * @param companyId the active company ID
     * @param branchId the branch identifier
     * @return the branch if it belongs to that company, empty otherwise
     */
    Optional<Branch> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long branchId);

    /**
     * Finds a branch by company and unique code.
     *
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @param branchCode the branch code
     * @return the branch if found, empty otherwise
     */
    Optional<Branch> findByTenantIdAndCompanyIdAndBranchCode(Long tenantId, Long companyId, String branchCode);

    /**
     * Finds a branch by tenant ID and branch code.
     *
     * <p>Used for lookups during branch creation and validation.
     *
     * @param tenantId the tenant ID
     * @param branchCode the branch code
     * @return the branch if found, empty otherwise
     */
    Optional<Branch> findByTenantIdAndBranchCode(Long tenantId, String branchCode);

    /**
     * Finds a branch by tenant ID and branch name.
     *
     * <p>Used for lookups during branch creation and validation.
     *
     * @param tenantId the tenant ID
     * @param branchName the branch name
     * @return the branch if found, empty otherwise
     */
    Optional<Branch> findByTenantIdAndBranchName(Long tenantId, String branchName);

    /**
     * Finds all branches for a specific tenant.
     *
     * @param tenantId the tenant ID
     * @return list of branches for the tenant
     */
    List<Branch> findByTenantId(Long tenantId);

    /**
     * Finds all branches belonging to one company inside a tenant.
     *
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @return list of branches for the company
     */
    List<Branch> findByTenantIdAndCompanyId(Long tenantId, Long companyId);

    /**
     * Finds all branches belonging to one company, paginated.
     *
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @param pageable the pagination parameters
     * @return a page of branches for the company
     */
    Page<Branch> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    /**
     * Finds all branches for a specific tenant, paginated.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination parameters
     * @return a page of branches for the tenant
     */
    Page<Branch> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Finds all active branches for a specific tenant.
     *
     * <p>Used for operational queries and dropdown selections.
     *
     * @param tenantId the tenant ID
     * @return list of active branches for the tenant
     */
    List<Branch> findByTenantIdAndStatus(Long tenantId, BranchStatus status);

    /**
     * Finds all active branches of the active company.
     *
     * <p>Used for dropdown selections that must not leak branches belonging to
     * another company of the same tenant.
     *
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @return list of active branches for the company
     */
    List<Branch> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId, BranchStatus status);

    /**
     * Finds the given branches, restricted to a single tenant.
     *
     * <p>Fallback used when no company is selected in the request context.
     *
     * @param branchIds the branch identifiers to load
     * @param tenantId the tenant ID
     * @return the branches owned by the tenant, never another tenant's rows
     */
    List<Branch> findAllByIdInAndTenantId(java.util.Collection<Long> branchIds, Long tenantId);

    /**
     * Finds the given branches, restricted to a single tenant and company.
     *
     * <p>Used to resolve the branches referenced by a page of user-branch
     * assignments in one query instead of one {@code findById} per row, while
     * still keeping other companies of the same tenant out of the result.
     *
     * @param branchIds the branch identifiers to load
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @return the branches owned by that company, never another company's rows
     */
    List<Branch> findAllByIdInAndTenantIdAndCompanyId(
            java.util.Collection<Long> branchIds, Long tenantId, Long companyId);

    /**
     * Checks if a branch code exists within a tenant.
     *
     * @param tenantId the tenant ID
     * @param branchCode the branch code to check
     * @return true if the branch code exists, false otherwise
     */
    boolean existsByTenantIdAndBranchCode(Long tenantId, String branchCode);

    /**
     * Checks if a branch name exists within a tenant.
     *
     * @param tenantId the tenant ID
     * @param branchName the branch name to check
     * @return true if the branch name exists, false otherwise
     */
    boolean existsByTenantIdAndBranchName(Long tenantId, String branchName);

    /**
     * Checks if a branch code already exists inside the given company.
     *
     * <p>Uniqueness is per company: two companies of the same tenant may each
     * have a branch called {@code HEAD-OFFICE}.
     *
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @param branchCode the branch code to check
     * @return true if the branch code exists for that company
     */
    boolean existsByTenantIdAndCompanyIdAndBranchCode(Long tenantId, Long companyId, String branchCode);

    /**
     * Checks if a branch name already exists inside the given company.
     *
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @param branchName the branch name to check
     * @return true if the branch name exists for that company
     */
    boolean existsByTenantIdAndCompanyIdAndBranchName(Long tenantId, Long companyId, String branchName);

    /**
     * Counts the number of branches for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the count of branches
     */
    long countByTenantId(Long tenantId);

    /**
     * Counts the number of active branches for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the count of active branches
     */
    long countByTenantIdAndStatus(Long tenantId, BranchStatus status);

    /**
     * Finds branches by tenant and status.
     *
     * @param tenantId the tenant ID
     * @param status the branch status
     * @return list of branches with the specified status
     */
    List<Branch> findByTenantIdAndStatusOrderByBranchName(Long tenantId, BranchStatus status);

    /**
     * Finds branches of one company by tenant and status, ordered by name.
     *
     * @param tenantId the tenant ID
     * @param companyId the company ID
     * @param status the branch status
     * @return list of branches with the specified status
     */
    List<Branch> findByTenantIdAndCompanyIdAndStatusOrderByBranchName(Long tenantId, Long companyId, BranchStatus status);
}
