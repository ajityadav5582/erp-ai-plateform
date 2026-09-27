package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * UserBranch association entity for many-to-many User-Branch relationship.
 *
 * <p>Represents the assignment of a User to a Branch with additional metadata.
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
 *   <li>A user can be assigned to multiple branches</li>
 *   <li>A branch can have multiple users</li>
 *   <li>Each (user, branch) pair must be unique</li>
 *   <li>User and branch must belong to the same tenant</li>
 *   <li>Only active branches can have new user assignments</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "user_branches",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_branch_access",
                columnNames = {"tenant_id", "user_id", "branch_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class UserBranch extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the user branch assignment.
     * Used for API access, external references, and audit trails.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "user_branch_id", nullable = false, unique = true, updatable = false)
    private UUID userBranchId;

    /**
     * Reference to the user.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Reference to the branch.
     */
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /**
     * Reference to the tenant this assignment belongs to.
     * Ensures multi-tenant data isolation.
     * Must match the tenant of both the user and the branch.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * The user or system that assigned this branch to the user.
     * Typically the user ID of the administrator who performed the assignment.
     * For system assignments, this may be a special system identifier.
     */
    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    /**
     * Timestamp when the branch was assigned to the user.
     */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    /**
     * Indicates if this is the primary branch for the user.
     * A user can have at most one primary branch.
     * Primary branch is used for default operations and UI display.
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    // ==============
    // Factory Methods
    // ==============

    /**
     * Factory method to create a new user branch assignment.
     *
     * <p>The application layer is responsible for providing the userBranchId.
     * This ensures proper UUID generation at the application layer.
     *
     * @param userBranchId the unique user branch assignment identifier (UUID)
     * @param userId the user ID
     * @param branchId the branch ID
     * @param tenantId the tenant ID
     * @param assignedBy the user or system that assigned the branch
     * @param assignedAt the timestamp when the assignment occurred
     * @param isPrimary whether this is the primary branch for the user
     * @return a new UserBranch instance
     */
    public static UserBranch assign(
            UUID userBranchId,
            Long userId,
            Long branchId,
            Long tenantId,
            String assignedBy,
            Instant assignedAt,
            Boolean isPrimary) {
        return UserBranch.builder()
                .userBranchId(userBranchId)
                .userId(userId)
                .branchId(branchId)
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
     * Sets this branch as the primary branch for the user.
     */
    public void setAsPrimary() {
        this.isPrimary = true;
    }

    /**
     * Removes the primary designation from this branch.
     */
    public void removePrimaryDesignation() {
        this.isPrimary = false;
    }

    /**
     * Checks if this assignment is for the specified user and branch.
     *
     * @param userId the user ID to check
     * @param branchId the branch ID to check
     * @return true if this assignment matches both user and branch
     */
    public boolean isFor(Long userId, Long branchId) {
        return this.userId.equals(userId) && this.branchId.equals(branchId);
    }

    /**
     * Checks if this is the primary branch for the user.
     *
     * @return true if this is the primary branch
     */
    public boolean isPrimary() {
        return isPrimary;
    }
}
