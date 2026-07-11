package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * RolePermission association entity for many-to-many Role-Permission relationship.
 *
 * <p>Represents the assignment of a Permission to a Role with audit metadata.
 * This is an association class that enriches the many-to-many relationship
 * with assignment tracking for compliance and audit purposes.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates assignment business logic and invariants</li>
 *   <li>Supports future auditing through inherited audit fields</li>
 *   <li>Provides domain behavior methods for assignment lifecycle</li>
 * </ul>
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>A Role can have many Permissions</li>
 *   <li>A Permission can belong to many Roles</li>
 *   <li>Each (role, permission) pair must be unique</li>
 *   <li>Only active permissions can be assigned to roles</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "role_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class RolePermission extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the role permission assignment.
     * Used for API access, external references, and audit trails.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "role_permission_id", nullable = false, unique = true, updatable = false)
    private UUID rolePermissionId;

    /**
     * Reference to the role.
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * Reference to the permission.
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * The user or system that assigned this permission to the role.
     * Typically the user ID of the administrator who performed the assignment.
     * For system assignments, this may be a special system identifier.
     */
    @Column(name = "assigned_by", nullable = false, length = 100)
    private String assignedBy;

    /**
     * Timestamp when the permission was assigned to the role.
     */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    // ==============
    // Factory Methods
    // ==============

    /**
     * Factory method to create a new role permission assignment.
     *
     * <p>The application layer is responsible for providing the rolePermissionId.
     * This ensures proper UUID generation at the application layer.
     *
     * @param rolePermissionId the unique role permission assignment identifier (UUID)
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @param assignedBy the user or system that assigned the permission
     * @param assignedAt the timestamp when the assignment occurred
     * @return a new RolePermission instance
     */
    public static RolePermission assign(
            UUID rolePermissionId,
            Long roleId,
            Long permissionId,
            String assignedBy,
            Instant assignedAt) {
        return RolePermission.builder()
                .rolePermissionId(rolePermissionId)
                .roleId(roleId)
                .permissionId(permissionId)
                .assignedBy(assignedBy)
                .assignedAt(assignedAt)
                .build();
    }

    // ==============
    // Query Methods
    // ==============

    /**
     * Checks if this assignment is for the given role and permission.
     *
     * @param roleId the role ID to check
     * @param permissionId the permission ID to check
     * @return true if this assignment matches, false otherwise
     */
    public boolean isFor(Long roleId, Long permissionId) {
        return this.roleId.equals(roleId) && this.permissionId.equals(permissionId);
    }
}
