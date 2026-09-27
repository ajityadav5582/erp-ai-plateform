package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * UserDepartment association entity for many-to-many User-Department relationship.
 *
 * <p>Represents the assignment of a User to a Department with additional metadata.
 * This is an association class that enriches the many-to-many relationship
 * with assignment details for audit and organizational purposes.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates assignment business logic and invariants</li>
 *   <li>Supports auditing through inherited audit fields</li>
 *   <li>Provides domain behavior methods for assignment lifecycle</li>
 * </ul>
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>A user can be assigned to multiple departments</li>
 *   <li>A department can have multiple users</li>
 *   <li>Each (user, department) pair must be unique</li>
 *   <li>User and department must belong to the same tenant</li>
 *   <li>Only active departments can have new user assignments</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class UserDepartment extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the user department assignment.
     * Used for API access, external references, and audit trails.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "user_department_id", nullable = false, unique = true, updatable = false)
    private UUID userDepartmentId;

    /**
     * Reference to the user.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Reference to the department.
     */
    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    /**
     * Reference to the tenant this assignment belongs to.
     * Ensures multi-tenant data isolation.
     * Must match the tenant of both the user and the department.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * The user or system that assigned this department to the user.
     * Typically the user ID of the administrator who performed the assignment.
     * For system assignments, this may be a special system identifier.
     */
    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    /**
     * Timestamp when the department was assigned to the user.
     */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    /**
     * Indicates if this is the primary department for the user.
     * A user can have at most one primary department.
     * Primary department is used for default operations and UI display.
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    // ==============
    // Factory Methods
    // ==============

    /**
     * Factory method to create a new user department assignment.
     *
     * <p>The application layer is responsible for providing the userDepartmentId.
     * This ensures proper UUID generation at the application layer.
     *
     * @param userDepartmentId the unique user department assignment identifier (UUID)
     * @param userId the user ID
     * @param departmentId the department ID
     * @param tenantId the tenant ID
     * @param assignedBy the user or system that assigned the department
     * @param assignedAt the timestamp when the assignment occurred
     * @param isPrimary whether this is the primary department for the user
     * @return a new UserDepartment instance
     */
    public static UserDepartment assign(
            UUID userDepartmentId,
            Long userId,
            Long departmentId,
            Long tenantId,
            String assignedBy,
            Instant assignedAt,
            Boolean isPrimary) {
        return UserDepartment.builder()
                .userDepartmentId(userDepartmentId)
                .userId(userId)
                .departmentId(departmentId)
                .tenantId(tenantId)
                .assignedBy(assignedBy)
                .assignedAt(assignedAt)
                .isPrimary(isPrimary != null ? isPrimary : false)
                .build();
    }

    // ==============
    // Business Methods
    // ==============

    /**
     * Sets this department as the primary department for the user.
     */
    public void setAsPrimary() {
        this.isPrimary = true;
    }

    /**
     * Removes the primary designation from this department.
     */
    public void removePrimaryDesignation() {
        this.isPrimary = false;
    }

    /**
     * Checks if this assignment is for the specified user and department.
     *
     * @param userId the user ID to check
     * @param departmentId the department ID to check
     * @return true if this assignment matches both user and department
     */
    public boolean isFor(Long userId, Long departmentId) {
        return this.userId.equals(userId) && this.departmentId.equals(departmentId);
    }

    /**
     * Checks if this is the primary department for the user.
     *
     * @return true if this is the primary department
     */
    public boolean isPrimary() {
        return isPrimary;
    }
}
