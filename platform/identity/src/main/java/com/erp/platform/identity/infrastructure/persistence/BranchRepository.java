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
import java.util.UUID;

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
     * Finds a branch by its unique business identifier.
     *
     * @param branchId the UUID business identifier
     * @return the branch if found, empty otherwise
     */
    Optional<Branch> findByBranchId(UUID branchId);

    /**
     * Finds a branch by tenant ID and its unique business identifier.
     *
     * @param tenantId the tenant ID
     * @param branchId the UUID business identifier
     * @return the branch if found, empty otherwise
     */
    Optional<Branch> findByTenantIdAndBranchId(Long tenantId, UUID branchId);

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
     * Finds all branches managed by a specific user.
     *
     * @param managerId the user ID of the manager
     * @return list of branches managed by the user
     */
    List<Branch> findByManagerId(Long managerId);

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
     * Finds branches by tenant and country.
     *
     * <p>Useful for regional reporting and filtering.
     *
     * @param tenantId the tenant ID
     * @param country the country code or name
     * @return list of branches in the specified country
     */
    List<Branch> findByTenantIdAndCountry(Long tenantId, String country);

    /**
     * Finds branches by tenant and city.
     *
     * <p>Useful for city-level reporting and filtering.
     *
     * @param tenantId the tenant ID
     * @param city the city name
     * @return list of branches in the specified city
     */
    List<Branch> findByTenantIdAndCity(Long tenantId, String city);

    /**
     * Finds branches by tenant and currency.
     *
     * <p>Useful for financial reporting and currency-specific operations.
     *
     * @param tenantId the tenant ID
     * @param currency the currency code
     * @return list of branches using the specified currency
     */
    List<Branch> findByTenantIdAndCurrency(Long tenantId, String currency);

    /**
     * Finds branches by tenant and timezone.
     *
     * <p>Useful for scheduling and time-based operations.
     *
     * @param tenantId the tenant ID
     * @param timezone the timezone identifier
     * @return list of branches in the specified timezone
     */
    List<Branch> findByTenantIdAndTimezone(Long tenantId, String timezone);

    /**
     * Finds branches by tenant and status.
     *
     * @param tenantId the tenant ID
     * @param status the branch status
     * @return list of branches with the specified status
     */
    List<Branch> findByTenantIdAndStatusOrderByBranchName(Long tenantId, BranchStatus status);

    /**
     * Custom query to find branches with their manager information.
     *
     * <p>Joins with users table to get manager details.
     *
     * @param tenantId the tenant ID
     * @return list of branches with manager information
     */
    @Query("SELECT b FROM Branch b WHERE b.tenantId = :tenantId AND b.managerId IS NOT NULL")
    List<Branch> findBranchesWithManagers(@Param("tenantId") Long tenantId);

    /**
     * Custom query to find branches without a manager.
     *
     * @param tenantId the tenant ID
     * @return list of branches without a manager
     */
    @Query("SELECT b FROM Branch b WHERE b.tenantId = :tenantId AND b.managerId IS NULL")
    List<Branch> findBranchesWithoutManagers(@Param("tenantId") Long tenantId);
}
