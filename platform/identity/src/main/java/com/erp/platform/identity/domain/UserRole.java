package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * UserRole association entity for many-to-many User-Role relationship.
 *
 * <p>Represents the assignment of a Role to a User with additional metadata.
 * This is an association class that enriches the many-to-many relationship
 * with assignment details, expiration, and primary role designation.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates assignment business logic and invariants</li>
 *   <li>Supports future auditing through inherited audit fields</li>
 *   <li>Provides domain behavior methods for role assignment lifecycle</li>
 * </ul>
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>A user can have multiple roles</li>
 *   <li>A role can be assigned to multiple users</li>
 *   <li>A user can have at most one primary role</li>
 *   <li>Role assignments can have an expiration date (nullable)</li>
 *   <li>Expired assignments are logically inactive but preserved for audit</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class UserRole extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the user role assignment.
     * Used for API access, external references, and audit trails.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "user_role_id", nullable = false, unique = true, updatable = false)
    private UUID userRoleId;

    /**
     * Reference to the user.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Reference to the role.
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * Reference to the tenant this assignment belongs to.
     * Ensures multi-tenant data isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * The user or system that assigned this role.
     * Typically the user ID of the administrator who performed the assignment.
     * For system assignments, this may be a special system identifier.
     */
    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    /**
     * Timestamp when the role was assigned to the user.
     */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    /**
     * Timestamp when the role assignment expires.
     * Null indicates the assignment does not expire.
     * Once expired, the assignment is logically inactive but preserved for audit.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Indicates if this is the primary role for the user.
     * A user can have at most one primary role.
     * Primary role is used for default permissions and UI display.
     */
    @Column(name = "is_primary_role", nullable = false)
    private Boolean isPrimaryRole;

    /**
     * Timestamp when the assignment was revoked.
     * Null indicates the assignment is still active.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * The user or system that revoked this role assignment.
     */
    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    /**
     * Reason for revoking the role assignment.
     * Used for audit and compliance purposes.
     */
    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    // ==============
    // Factory Methods
    // ==============

    /**
     * Factory method to create a new user role assignment.
     *
     * <p>The application layer is responsible for providing the userRoleId.
     * This ensures proper UUID generation at the application layer.
     *
     * @param userRoleId the unique user role assignment identifier (UUID)
     * @param userId the user ID
     * @param roleId the role ID
     * @param tenantId the tenant ID
     * @param assignedBy the user or system that assigned the role
     * @param assignedAt the timestamp when the assignment occurred
     * @param expiresAt the expiration timestamp (null for non-expiring assignments)
     * @param isPrimaryRole whether this is the primary role for the user
     * @return a new UserRole instance
     */
    public static UserRole assign(
            UUID userRoleId,
            Long userId,
            Long roleId,
            Long tenantId,
            String assignedBy,
            Instant assignedAt,
            Instant expiresAt,
            Boolean isPrimaryRole) {
        return UserRole.builder()
                .userRoleId(userRoleId)
                .userId(userId)
                .roleId(roleId)
                .tenantId(tenantId)
                .assignedBy(assignedBy)
                .assignedAt(assignedAt)
                .expiresAt(expiresAt)
                .isPrimaryRole(isPrimaryRole != null ? isPrimaryRole : false)
                .build();
    }

    // ==============
    // Business Behavior
    // ==============

    /**
     * Revokes the role assignment.
     *
     * <p>Sets the revokedAt timestamp and records who revoked it and why.
     * The assignment is preserved for audit purposes.
     *
     * @param revokedAt the timestamp when revocation occurs
     * @param revokedBy the user or system that revoked the assignment
     * @param revokeReason the reason for revocation
     */
    public void revoke(Instant revokedAt, String revokedBy, String revokeReason) {
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
        this.revokeReason = revokeReason;
    }

    /**
     * Extends the expiration of the role assignment.
     *
     * <p>Only applicable to assignments that have not been revoked.
     *
     * @param newExpiresAt the new expiration timestamp
     * @throws IllegalStateException if the assignment has been revoked
     */
    public void extendExpiration(Instant newExpiresAt) {
        if (this.revokedAt != null) {
            throw new IllegalStateException("Cannot extend expiration of revoked assignment: " + this.userRoleId);
        }
        this.expiresAt = newExpiresAt;
    }

    /**
     * Removes the expiration from the role assignment.
     *
     * <p>Converts a time-limited assignment to a permanent one.
     *
     * @throws IllegalStateException if the assignment has been revoked
     */
    public void removeExpiration() {
        if (this.revokedAt != null) {
            throw new IllegalStateException("Cannot remove expiration of revoked assignment: " + this.userRoleId);
        }
        this.expiresAt = null;
    }

    /**
     * Sets this role as the primary role for the user.
     *
     * @throws IllegalStateException if the assignment has been revoked
     */
    public void setAsPrimary() {
        if (this.revokedAt != null) {
            throw new IllegalStateException("Cannot set revoked assignment as primary: " + this.userRoleId);
        }
        this.isPrimaryRole = true;
    }

    /**
     * Removes the primary role designation from this assignment.
     *
     * @throws IllegalStateException if the assignment has been revoked
     */
    public void removePrimaryDesignation() {
        if (this.revokedAt != null) {
            throw new IllegalStateException("Cannot remove primary designation from revoked assignment: " + this.userRoleId);
        }
        this.isPrimaryRole = false;
    }

    // ==============
    // Query Methods
    // ==============

    /**
     * Checks if the assignment is currently active.
     *
     * <p>An assignment is active if it has not been revoked and has not expired.
     *
     * @return true if the assignment is active, false otherwise
     */
    public boolean isActive() {
        if (this.revokedAt != null) {
            return false;
        }
        if (this.expiresAt != null && this.expiresAt.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the assignment has been revoked.
     *
     * @return true if the assignment has been revoked, false otherwise
     */
    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    /**
     * Checks if the assignment has expired.
     *
     * @return true if the assignment has expired, false otherwise
     */
    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(Instant.now());
    }

    /**
     * Checks if the assignment is permanent (does not expire).
     *
     * @return true if the assignment does not expire, false otherwise
     */
    public boolean isPermanent() {
        return this.expiresAt == null;
    }

    /**
     * Checks if this is the primary role for the user.
     *
     * @return true if this is the primary role, false otherwise
     */
    public boolean isPrimaryRole() {
        return Boolean.TRUE.equals(this.isPrimaryRole);
    }

    /**
     * Checks if the assignment can be revoked.
     *
     * <p>An assignment can be revoked if it is active and not already revoked.
     *
     * @return true if the assignment can be revoked, false otherwise
     */
    public boolean canBeRevoked() {
        return !isRevoked() && isActive();
    }

    /**
     * Checks if the assignment can be extended.
     *
     * <p>An assignment can be extended if it is active, not revoked, and has an expiration.
     *
     * @return true if the assignment can be extended, false otherwise
     */
    public boolean canBeExtended() {
        return !isRevoked() && isActive() && this.expiresAt != null;
    }
}
