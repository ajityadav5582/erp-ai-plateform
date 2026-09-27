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
}
