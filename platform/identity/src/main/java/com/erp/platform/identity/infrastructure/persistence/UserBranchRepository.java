package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.UserBranch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserBranch aggregate.
 *
 * <p>Provides data access methods for user-branch assignment operations.
 * All queries are tenant-scoped to ensure data isolation.
 *
 * @since 1.0.0
 */
public interface UserBranchRepository extends JpaRepository<UserBranch, Long> {

    /**
     * Finds a user-branch assignment by tenant ID, user ID, and branch ID.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param branchId the branch ID
     * @return the assignment if found, empty otherwise
     */
    Optional<UserBranch> findByTenantIdAndUserIdAndBranchId(Long tenantId, Long userId, Long branchId);

    /**
     * Finds all branch assignments for a specific user in a tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @return list of branch assignments for the user
     */
    List<UserBranch> findByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Finds all branch assignments for a specific user in a tenant, paginated.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param pageable the pagination parameters
     * @return a page of branch assignments for the user
     */
    Page<UserBranch> findByTenantIdAndUserId(Long tenantId, Long userId, Pageable pageable);

    /**
     * Finds all user assignments for a specific branch in a tenant.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @return list of user assignments for the branch
     */
    List<UserBranch> findByTenantIdAndBranchId(Long tenantId, Long branchId);

    /**
     * Finds all user assignments for a specific branch in a tenant, paginated.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @param pageable the pagination parameters
     * @return a page of user assignments for the branch
     */
    Page<UserBranch> findByTenantIdAndBranchId(Long tenantId, Long branchId, Pageable pageable);

    /**
     * Finds the primary branch assignment for a user in a tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @return the primary branch assignment if found, empty otherwise
     */
    Optional<UserBranch> findByTenantIdAndUserIdAndIsPrimaryTrue(Long tenantId, Long userId);

    /**
     * Checks if a user-branch assignment exists within a tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param branchId the branch ID
     * @return true if the assignment exists, false otherwise
     */
    boolean existsByTenantIdAndUserIdAndBranchId(Long tenantId, Long userId, Long branchId);

    /**
     * Counts the number of branches assigned to a user in a tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @return the count of branch assignments
     */
    long countByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Counts the number of users assigned to a branch in a tenant.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @return the count of user assignments
     */
    long countByTenantIdAndBranchId(Long tenantId, Long branchId);

    /**
     * Deletes all branch assignments for a user in a tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     */
    void deleteByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Deletes all user assignments for a branch in a tenant.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     */
    void deleteByTenantIdAndBranchId(Long tenantId, Long branchId);
}
