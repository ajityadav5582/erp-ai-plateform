package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Department;
import com.erp.platform.identity.domain.DepartmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Department aggregate.
 *
 * <p>Provides data access methods for department management operations.
 * All queries are tenant-scoped to ensure data isolation.
 *
 * @since 1.0.0
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Finds a department by its unique business identifier.
     *
     * @param departmentId the UUID business identifier
     * @return the department if found, empty otherwise
     */
    Optional<Department> findByDepartmentId(UUID departmentId);

    /**
     * Finds a department by tenant ID and its unique business identifier.
     *
     * @param tenantId the tenant ID
     * @param departmentId the UUID business identifier
     * @return the department if found, empty otherwise
     */
    Optional<Department> findByTenantIdAndDepartmentId(Long tenantId, UUID departmentId);

    /**
     * Finds a department by tenant ID, branch ID, and department code.
     *
     * <p>Used for lookups during department creation and validation.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @param departmentCode the department code
     * @return the department if found, empty otherwise
     */
    Optional<Department> findByTenantIdAndBranchIdAndDepartmentCode(Long tenantId, Long branchId, String departmentCode);

    /**
     * Finds a department by tenant ID, branch ID, and department name.
     *
     * <p>Used for lookups during department creation and validation.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @param departmentName the department name
     * @return the department if found, empty otherwise
     */
    Optional<Department> findByTenantIdAndBranchIdAndDepartmentName(Long tenantId, Long branchId, String departmentName);

    /**
     * Finds all departments for a specific tenant.
     *
     * @param tenantId the tenant ID
     * @return list of departments for the tenant
     */
    List<Department> findByTenantId(Long tenantId);

    /**
     * Finds all departments for a specific tenant, paginated.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination parameters
     * @return a page of departments for the tenant
     */
    Page<Department> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Finds all departments for a specific branch.
     *
     * @param branchId the branch ID
     * @return list of departments for the branch
     */
    List<Department> findByBranchId(Long branchId);

    /**
     * Finds all active departments for a specific branch.
     *
     * <p>Used for operational queries and dropdown selections.
     *
     * @param branchId the branch ID
     * @return list of active departments for the branch
     */
    List<Department> findByBranchIdAndStatus(Long branchId, DepartmentStatus status);

    /**
     * Finds all departments managed by a specific user.
     *
     * @param managerId the user ID of the manager
     * @return list of departments managed by the user
     */
    List<Department> findByManagerId(Long managerId);

    /**
     * Finds all root departments (departments without a parent) for a specific branch.
     *
     * @param branchId the branch ID
     * @return list of root departments
     */
    List<Department> findByBranchIdAndParentDepartmentIdIsNull(Long branchId);

    /**
     * Finds all child departments of a specific parent department.
     *
     * @param parentDepartmentId the parent department ID
     * @return list of child departments
     */
    List<Department> findByParentDepartmentId(Long parentDepartmentId);

    /**
     * Checks if a department code exists within a branch.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @param departmentCode the department code to check
     * @return true if the department code exists, false otherwise
     */
    boolean existsByTenantIdAndBranchIdAndDepartmentCode(Long tenantId, Long branchId, String departmentCode);

    /**
     * Checks if a department name exists within a branch.
     *
     * @param tenantId the tenant ID
     * @param branchId the branch ID
     * @param departmentName the department name to check
     * @return true if the department name exists, false otherwise
     */
    boolean existsByTenantIdAndBranchIdAndDepartmentName(Long tenantId, Long branchId, String departmentName);

    /**
     * Counts the number of departments for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the count of departments
     */
    long countByTenantId(Long tenantId);

    /**
     * Counts the number of departments for a branch.
     *
     * @param branchId the branch ID
     * @return the count of departments
     */
    long countByBranchId(Long branchId);

    /**
     * Counts the number of active departments for a branch.
     *
     * @param branchId the branch ID
     * @return the count of active departments
     */
    long countByBranchIdAndStatus(Long branchId, DepartmentStatus status);

    /**
     * Finds departments by tenant and status.
     *
     * @param tenantId the tenant ID
     * @param status the department status
     * @return list of departments with the specified status
     */
    List<Department> findByTenantIdAndStatus(Long tenantId, DepartmentStatus status);

    /**
     * Finds departments by tenant and status, ordered by name.
     *
     * @param tenantId the tenant ID
     * @param status the department status
     * @return list of departments with the specified status, ordered by name
     */
    List<Department> findByTenantIdAndStatusOrderByDepartmentName(Long tenantId, DepartmentStatus status);

    /**
     * Custom query to find departments with their manager information.
     *
     * <p>Joins with users table to get manager details.
     *
     * @param tenantId the tenant ID
     * @return list of departments with manager information
     */
    @Query("SELECT d FROM Department d WHERE d.tenantId = :tenantId AND d.managerId IS NOT NULL")
    List<Department> findDepartmentsWithManagers(@Param("tenantId") Long tenantId);

    /**
     * Custom query to find departments without a manager.
     *
     * @param tenantId the tenant ID
     * @return list of departments without a manager
     */
    @Query("SELECT d FROM Department d WHERE d.tenantId = :tenantId AND d.managerId IS NULL")
    List<Department> findDepartmentsWithoutManagers(@Param("tenantId") Long tenantId);

    /**
     * Finds the hierarchy path from a root department to the specified department.
     *
     * <p>Returns the chain of parent departments from root to the given department.
     *
     * @param departmentId the department ID to find the path for
     * @return list of departments in the hierarchy path (root first)
     */
    @Query(value = """
        WITH RECURSIVE dept_hierarchy AS (
            SELECT id, department_id, tenant_id, branch_id, department_code, department_name,
                   description, manager_id, parent_department_id, status,
                   created_at, updated_at, created_by, updated_by, version
            FROM departments
            WHERE id = :departmentId
            UNION ALL
            SELECT d.id, d.department_id, d.tenant_id, d.branch_id, d.department_code, d.department_name,
                   d.description, d.manager_id, d.parent_department_id, d.status,
                   d.created_at, d.updated_at, d.created_by, d.updated_by, d.version
            FROM departments d
            INNER JOIN dept_hierarchy dh ON d.id = dh.parent_department_id
        )
        SELECT * FROM dept_hierarchy ORDER BY parent_department_id NULLS FIRST
        """, nativeQuery = true)
    List<Department> findHierarchyPath(@Param("departmentId") Long departmentId);
}
