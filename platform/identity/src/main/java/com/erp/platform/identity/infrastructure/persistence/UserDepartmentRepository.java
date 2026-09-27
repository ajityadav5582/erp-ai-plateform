package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.UserDepartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for UserDepartment aggregate.
 *
 * <p>Provides data access methods for user-department assignment operations.
 * All queries are tenant-scoped to ensure data isolation.
 *
 * @since 1.0.0
 */
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {

    /**
     * Finds a user department assignment by tenant ID, user ID, and department ID.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param departmentId the department ID
     * @return the assignment if found, empty otherwise
     */
    Optional<UserDepartment> findByTenantIdAndUserIdAndDepartmentId(Long tenantId, Long userId, Long departmentId);

    /**
     * Finds all department assignments for a specific user in a tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @return list of department assignments for the user
     */
    List<UserDepartment> findByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Finds all department assignments for a specific user in a tenant, paginated.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param pageable the pagination parameters
     * @return a page of department assignments for the user
     */
    Page<UserDepartment> findByTenantIdAndUserId(Long tenantId, Long userId, Pageable pageable);

    /**
     * Finds all user assignments for a specific department in a tenant.
     *
     * @param tenantId the tenant ID
     * @param departmentId the department ID
     * @return list of user assignments for the department
     */
    List<UserDepartment> findByTenantIdAndDepartmentId(Long tenantId, Long departmentId);

    /**
     * Finds all user assignments for a specific department in a tenant, paginated.
     *
     * @param tenantId the tenant ID
     * @param departmentId the department ID
     * @param pageable the pagination parameters
     * @return a page of user assignments for the department
     */
    Page<UserDepartment> findByTenantIdAndDepartmentId(Long tenantId, Long departmentId, Pageable pageable);

    /**
     * Checks if a user department assignment exists.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     * @param departmentId the department ID
     * @return true if the assignment exists, false otherwise
     */
    boolean existsByTenantIdAndUserIdAndDepartmentId(Long tenantId, Long userId, Long departmentId);

    /**
     * Deletes all department assignments for a specific user in a tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID
     */
    void deleteByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Deletes all user assignments for a specific department in a tenant.
     *
     * @param tenantId the tenant ID
     * @param departmentId the department ID
     */
    void deleteByTenantIdAndDepartmentId(Long tenantId, Long departmentId);
}
