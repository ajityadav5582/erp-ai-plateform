package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import com.erp.platform.identity.domain.exception.CannotActivateDepartmentException;
import com.erp.platform.identity.domain.exception.CannotDeactivateDepartmentException;
import com.erp.platform.identity.domain.exception.CannotMoveDepartmentException;
import com.erp.platform.identity.domain.exception.CircularDepartmentHierarchyException;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Department aggregate root for organizational unit management.
 *
 * <p>Represents a functional unit within a Branch of a Tenant.
 * Departments can be organized hierarchically with parent-child relationships.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates all department business logic and invariants</li>
 *   <li>Does not expose mutable state directly (no setters for business fields)</li>
 *   <li>Provides domain behavior methods for state transitions</li>
 * </ul>
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>Department code must be unique within a Branch</li>
 *   <li>Department name must be unique within a Branch</li>
 *   <li>A department belongs to exactly one tenant and one branch</li>
 *   <li>Hierarchical departments are supported via parentDepartmentId</li>
 *   <li>Circular parent-child relationships are prevented</li>
 *   <li>Departments with child departments cannot be deleted</li>
 *   <li>Departments with assigned users cannot be deleted</li>
 * </ul>
 *
 * <p><strong>Future Extensibility:</strong>
 * <p>The schema is designed to support future assignment of:
 * <ul>
 *   <li>Teams (via team.department_id)</li>
 *   <li>Users (via user.department_id)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Department extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the department.
     * Used for API access, external references, and integration.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "department_id", nullable = false, unique = true, updatable = false)
    private UUID departmentId;

    /**
     * The tenant this department belongs to.
     * A department cannot belong to multiple tenants.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * The branch this department belongs to.
     * A department cannot belong to multiple branches.
     */
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /**
     * Unique department code within the branch.
     * Used for display, reporting, and integration.
     * Must be unique within the branch scope.
     */
    @Column(name = "department_code", nullable = false, length = 50)
    private String departmentCode;

    /**
     * Unique department name within the branch.
     * Human-readable name for the department.
     * Must be unique within the branch scope.
     */
    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    /**
     * Detailed description of the department's purpose and responsibilities.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * The user ID of the department manager.
     * References the user who manages this department.
     * Nullable until a manager is assigned.
     */
    @Column(name = "manager_id")
    private Long managerId;

    /**
     * The parent department ID for hierarchical organization.
     * Null for root-level departments.
     */
    @Column(name = "parent_department_id")
    private Long parentDepartmentId;

    /**
     * Current status of the department.
     * Determines whether the department is operational.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DepartmentStatus status;

    // ==================== Domain Methods ====================

    /**
     * Activates the department, making it operational.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Department must not already be active</li>
     *   <li>Department must have a valid tenant and branch</li>
     * </ul>
     *
     * @throws CannotActivateDepartmentException if department is already active
     */
    public void activate() {
        if (this.status == DepartmentStatus.ACTIVE) {
            throw new CannotActivateDepartmentException("Department is already active");
        }
        this.status = DepartmentStatus.ACTIVE;
    }

    /**
     * Deactivates the department, making it non-operational.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>Department must be active to be deactivated</li>
     *   <li>Deactivation preserves all historical data for audit</li>
     * </ul>
     *
     * @throws CannotDeactivateDepartmentException if department is not active
     */
    public void deactivate() {
        if (this.status != DepartmentStatus.ACTIVE) {
            throw new CannotDeactivateDepartmentException("Only active departments can be deactivated");
        }
        this.status = DepartmentStatus.INACTIVE;
    }

    /**
     * Changes the manager of the department.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>New manager must be a valid user ID</li>
     *   <li>Manager can be set to null (unassigned)</li>
     * </ul>
     *
     * @param managerId the user ID of the new manager, or null to unassign
     * @throws IllegalArgumentException if managerId is negative
     */
    public void changeManager(Long managerId) {
        if (managerId != null && managerId <= 0) {
            throw new IllegalArgumentException("Manager ID must be a positive number");
        }
        this.managerId = managerId;
    }

    /**
     * Moves the department to a different branch.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>New branch must be different from current branch</li>
     *   <li>Department code must be unique in the new branch</li>
     *   <li>Department name must be unique in the new branch</li>
     * </ul>
     *
     * @param newBranchId the ID of the new branch
     * @throws CannotMoveDepartmentException if moving to the same branch
     */
    public void moveToBranch(Long newBranchId) {
        if (this.branchId.equals(newBranchId)) {
            throw new CannotMoveDepartmentException("Department is already in the specified branch");
        }
        this.branchId = newBranchId;
    }

    /**
     * Moves the department to a different parent department.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>New parent must be different from current parent</li>
     *   <li>Circular parent-child relationships are prevented</li>
     *   <li>Department cannot be its own parent</li>
     * </ul>
     *
     * @param newParentDepartmentId the ID of the new parent department, or null to make it a root department
     * @param allDepartments list of all departments in the tenant for circular reference validation
     * @throws CircularDepartmentHierarchyException if circular reference is detected
     * @throws CannotMoveDepartmentException if moving to the same parent
     */
    public void moveToParent(Long newParentDepartmentId, List<Department> allDepartments) {
        if (this.parentDepartmentId != null && this.parentDepartmentId.equals(newParentDepartmentId)) {
            throw new CannotMoveDepartmentException("Department already has the specified parent");
        }

        if (newParentDepartmentId != null) {
            // Prevent self-reference
            if (getId() != null && getId().equals(newParentDepartmentId)) {
                throw new CircularDepartmentHierarchyException("Department cannot be its own parent");
            }

            // Prevent circular references
            if (allDepartments != null) {
                validateNoCircularReference(newParentDepartmentId, allDepartments);
            }
        }

        this.parentDepartmentId = newParentDepartmentId;
    }

    /**
     * Checks if this department is a root department (has no parent).
     *
     * @return true if this is a root department, false otherwise
     */
    public boolean isRootDepartment() {
        return this.parentDepartmentId == null;
    }

    /**
     * Validates that moving to a new parent would not create a circular reference.
     *
     * <p>This method traverses the hierarchy to ensure the new parent
     * is not a descendant of this department.
     *
     * @param newParentId the ID of the proposed new parent
     * @param allDepartments list of all departments in the tenant
     * @throws CircularDepartmentHierarchyException if circular reference is detected
     */
    private void validateNoCircularReference(Long newParentId, List<Department> allDepartments) {
        if (getId() == null) {
            return;
        }

        // Build a map for quick lookup
        java.util.Map<Long, Department> departmentMap = new java.util.HashMap<>();
        for (Department dept : allDepartments) {
            departmentMap.put(dept.getId(), dept);
        }

        // Traverse up from the new parent to check if we reach this department
        Long currentId = newParentId;
        while (currentId != null) {
            if (getId().equals(currentId)) {
                throw new CircularDepartmentHierarchyException(
                    "Circular reference detected: cannot move department under its own descendant"
                );
            }
            Department parent = departmentMap.get(currentId);
            if (parent == null) {
                break;
            }
            currentId = parent.getParentDepartmentId();
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a new Department instance.
     *
     * <p>Business Rules:
     * <ul>
     *   <li>tenantId must not be null</li>
     *   <li>branchId must not be null</li>
     *   <li>departmentCode must not be blank</li>
     *   <li>departmentName must not be blank</li>
     *   <li>departmentId is auto-generated</li>
     *   <li>status defaults to ACTIVE</li>
     * </ul>
     *
     * @param tenantId the tenant this department belongs to
     * @param branchId the branch this department belongs to
     * @param departmentCode the unique department code within the branch
     * @param departmentName the unique department name within the branch
     * @return a new Department instance
     * @throws IllegalArgumentException if any required field is invalid
     */
    public static Department create(Long tenantId, Long branchId, String departmentCode, String departmentName) {
        validateTenantId(tenantId);
        validateBranchId(branchId);
        validateDepartmentCode(departmentCode);
        validateDepartmentName(departmentName);

        return Department.builder()
                .departmentId(UUID.randomUUID())
                .tenantId(tenantId)
                .branchId(branchId)
                .departmentCode(departmentCode.trim().toUpperCase())
                .departmentName(departmentName.trim())
                .status(DepartmentStatus.ACTIVE)
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }
    }

    private static void validateBranchId(Long branchId) {
        if (branchId == null || branchId <= 0) {
            throw new IllegalArgumentException("Branch ID must be a positive number");
        }
    }

    private static void validateDepartmentCode(String departmentCode) {
        if (departmentCode == null || departmentCode.isBlank()) {
            throw new IllegalArgumentException("Department code must not be blank");
        }
        if (departmentCode.length() > 50) {
            throw new IllegalArgumentException("Department code must not exceed 50 characters");
        }
    }

    private static void validateDepartmentName(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            throw new IllegalArgumentException("Department name must not be blank");
        }
        if (departmentName.length() > 200) {
            throw new IllegalArgumentException("Department name must not exceed 200 characters");
        }
    }
}
