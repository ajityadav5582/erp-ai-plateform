package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Role aggregate root for role-based access control (RBAC).
 *
 * <p>Represents a role in the multi-tenant ERP platform.
 * Roles define permissions and access levels that can be assigned to users.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates all role business logic and invariants</li>
 *   <li>Does not expose mutable state directly (no setters for business fields)</li>
 *   <li>Provides domain behavior methods for state transitions</li>
 * </ul>
 *
 * <p><strong>Role Types:</strong>
 * <ul>
 *   <li>SYSTEM: Predefined roles that cannot be modified or deleted (e.g., SUPER_ADMIN, TENANT_ADMIN)</li>
 *   <li>CUSTOM: Tenant-defined roles that can be created, modified, and deleted</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Role extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the role.
     * Used for API access, external references, and audit trails.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "role_id", nullable = false, unique = true, updatable = false)
    private UUID roleId;

    /**
     * Reference to the tenant this role belongs to.
     * Ensures multi-tenant data isolation.
     * Null for system roles that are shared across all tenants.
     */
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * Unique role code within the tenant (or globally for system roles).
     * Used for programmatic access and API references.
     * Examples: "SUPER_ADMIN", "FINANCE_MANAGER", "SALES_REP"
     */
    @Column(name = "role_code", nullable = false, length = 100)
    private String roleCode;

    /**
     * Display name of the role.
     * Used in UI and user-facing contexts.
     */
    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    /**
     * Detailed description of the role and its permissions.
     * Used for documentation and UI display.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Type of the role.
     * SYSTEM roles are predefined and cannot be modified or deleted.
     * CUSTOM roles are tenant-defined and can be managed by administrators.
     */
    @Column(name = "role_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    /**
     * Indicates if this is a system role.
     * System roles are predefined and cannot be modified or deleted.
     * @deprecated Use {@link #roleType} instead
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    @Column(name = "is_system_role", nullable = false)
    private Boolean isSystemRole;

    /**
     * Timestamp when the role was deactivated.
     * Set when transitioning to INACTIVE status.
     */
    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    // ==============
    // Factory Methods
    // ==============

    /**
     * Factory method to create a new custom role.
     *
     * <p>The application layer is responsible for providing the roleId.
     * This ensures proper UUID generation at the application layer.
     *
     * @param roleId the unique role identifier (UUID)
     * @param tenantId the tenant this role belongs to (null for system roles)
     * @param roleCode the unique role code
     * @param roleName the display name of the role
     * @param description the role description
     * @return a new Role instance
     */
    public static Role createCustom(
            UUID roleId,
            Long tenantId,
            String roleCode,
            String roleName,
            String description) {
        return Role.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .roleCode(roleCode)
                .roleName(roleName)
                .description(description)
                .roleType(RoleType.CUSTOM)
                .isSystemRole(false)
                .build();
    }

    /**
     * Factory method to create a new system role.
     *
     * <p>System roles are predefined and shared across all tenants.
     *
     * @param roleId the unique role identifier (UUID)
     * @param roleCode the unique role code
     * @param roleName the display name of the role
     * @param description the role description
     * @return a new Role instance
     */
    public static Role createSystem(
            UUID roleId,
            String roleCode,
            String roleName,
            String description) {
        return Role.builder()
                .roleId(roleId)
                .tenantId(null)
                .roleCode(roleCode)
                .roleName(roleName)
                .description(description)
                .roleType(RoleType.SYSTEM)
                .isSystemRole(true)
                .build();
    }

    // ==============
    // Business Behavior
    // ==============

    /**
     * Updates the role's display information.
     *
     * <p>System roles cannot be modified.
     *
     * @param roleName the new display name
     * @param description the new description
     * @throws IllegalStateException if attempting to modify a system role
     */
    public void updateDetails(String roleName, String description) {
        if (this.roleType == RoleType.SYSTEM) {
            throw new IllegalStateException("Cannot modify system role: " + this.roleId);
        }

        if (roleName != null && !roleName.isBlank()) {
            this.roleName = roleName;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }

    /**
     * Deactivates the role.
     *
     * <p>System roles cannot be deactivated.
     *
     * @param deactivatedAt the timestamp when deactivation occurs
     * @throws IllegalStateException if attempting to deactivate a system role
     */
    public void deactivate(Instant deactivatedAt) {
        if (this.roleType == RoleType.SYSTEM) {
            throw new IllegalStateException("Cannot deactivate system role: " + this.roleId);
        }

        this.deactivatedAt = deactivatedAt;
    }

    /**
     * Reactivates a deactivated role.
     *
     * @throws IllegalStateException if attempting to reactivate a system role
     */
    public void reactivate() {
        if (this.roleType == RoleType.SYSTEM) {
            throw new IllegalStateException("Cannot reactivate system role: " + this.roleId);
        }

        this.deactivatedAt = null;
    }

    // ==============
    // Query Methods
    // ==============

    /**
     * Checks if this is a system role.
     *
     * @return true if this is a system role, false otherwise
     */
    public boolean isSystemRole() {
        return this.roleType == RoleType.SYSTEM;
    }

    /**
     * Checks if this is a custom role.
     *
     * @return true if this is a custom role, false otherwise
     */
    public boolean isCustomRole() {
        return this.roleType == RoleType.CUSTOM;
    }

    /**
     * Checks if the role is active (not deactivated).
     *
     * @return true if the role is active, false otherwise
     */
    public boolean isActive() {
        return this.deactivatedAt == null;
    }

    /**
     * Checks if the role is deactivated.
     *
     * @return true if the role is deactivated, false otherwise
     */
    public boolean isDeactivated() {
        return this.deactivatedAt != null;
    }

    /**
     * Checks if the role can be modified.
     *
     * <p>System roles cannot be modified.
     *
     * @return true if the role can be modified, false otherwise
     */
    public boolean canBeModified() {
        return this.roleType == RoleType.CUSTOM;
    }

    /**
     * Checks if the role can be deactivated.
     *
     * <p>System roles cannot be deactivated.
     *
     * @return true if the role can be deactivated, false otherwise
     */
    public boolean canBeDeactivated() {
        return this.roleType == RoleType.CUSTOM && this.deactivatedAt == null;
    }
}
