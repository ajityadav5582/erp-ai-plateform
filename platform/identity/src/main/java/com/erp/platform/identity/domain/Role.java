package com.erp.platform.identity.domain;

import com.erp.platform.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

/**
 * Role Aggregate Root for Role-Based Access Control (RBAC) and Attribute-Based Access Control (ABAC).
 *
 * <p>Represents a security role in the multi-tenant ERP platform.
 * As the true DDD Aggregate Root, {@code Role} strictly encapsulates its child
 * {@link RolePermission} entities and enforces all domain invariants regarding permission assignments.
 *
 * <p><strong>Architectural Directives Enforced:</strong>
 * <ul>
 *   <li>True DDD Aggregate Root: Encapsulates {@code Set<RolePermission>} mapped with
 *       {@code CascadeType.ALL} and {@code orphanRemoval = true}</li>
 *   <li>Domain Behavior Methods: {@link #assignPermission}, {@link #revokePermission},
 *       and {@link #syncPermissions} preserve domain invariants</li>
 *   <li>Domain Invariants Enforced: Verifies role editability (non-SYSTEM & editable) and checks
 *       permission status (must be ACTIVE) before mutating state</li>
 *   <li>Persists only the requested role fields while keeping permissions in their separate relation</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "roles")
@org.hibernate.annotations.Check(name = "chk_role_type", constraints = "role_type IN ('SYSTEM', 'TENANT', 'CUSTOM')")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Role {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * Unique role code within the tenant (e.g., "SUPER_ADMIN", "FINANCE_MANAGER").
     */
    @Column(name = "code", nullable = false, length = 100)
    private String roleCode;

    /**
     * Display name of the role.
     */
    @Column(name = "name", nullable = false, length = 150)
    private String roleName;

    /**
     * Detailed description of the role's purpose and scope.
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "role_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    /**
     * Current status of the role (ACTIVE, INACTIVE).
     */
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Encapsulated collection of permissions assigned to this role.
     * Managed exclusively through aggregate domain methods.
     */
    @SuppressWarnings("serial")
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<RolePermission> permissions = new HashSet<>();

    // ==================== Factory Methods ====================

    /**
     * Factory method to create a new custom tenant role.
     */
    public static Role createCustom(Long tenantId, String roleCode, String roleName, String description) {
        validateTenantAndRoleCode(tenantId, roleCode);
        return Role.builder()
                .tenantId(tenantId)
                .roleCode(roleCode.trim().toUpperCase())
                .roleName(roleName.trim())
                .description(description != null ? description.trim() : null)
                .roleType(RoleType.CUSTOM)
                .status(Status.ACTIVE)
                .permissions(new HashSet<>())
                .build();
    }

    /** Creates a tenant-scoped role provisioned by the platform. */
    public static Role createTenant(String roleCode, String roleName, String description, Long tenantId) {
        validateTenantAndRoleCode(tenantId, roleCode);
        return Role.builder()
                .tenantId(tenantId)
                .roleCode(roleCode.trim().toUpperCase())
                .roleName(roleName.trim())
                .description(description != null ? description.trim() : null)
                .roleType(RoleType.TENANT)
                .status(Status.ACTIVE)
                .permissions(new HashSet<>())
                .build();
    }

    /**
     * Factory method to create a platform system role.
     */
    public static Role createSystem(String roleCode, String roleName, String description) {
        validateRoleCode(roleCode);
        return Role.builder()
                .roleCode(roleCode.trim().toUpperCase())
                .roleName(roleName.trim())
                .description(description != null ? description.trim() : null)
                .roleType(RoleType.SYSTEM)
                .status(Status.ACTIVE)
                .permissions(new HashSet<>())
                .build();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Moves a legacy tenant-scoped system role to the global system role scope. */
    public void moveToGlobalSystemScope() {
        if (this.roleType != RoleType.SYSTEM) {
            throw new IllegalStateException("Only system roles can use the global scope");
        }
        this.tenantId = null;
    }

    // ==================== Aggregate Domain Behavior Methods ====================

    /**
     * Assigns a permission to this role with an explicit ABAC DataScope, preserving domain invariants.
     *
     * @param permission the permission entity to assign
     * @param dataScope the ABAC data scope (ALL, OWN_BRANCH, etc.)
     * @param assignedBy the user or actor performing the assignment
     * @return the created or updated {@link RolePermission} assignment
     */
    public RolePermission assignPermission(Permission permission, DataScope dataScope, String assignedBy) {
        verifyCanBeModified();
        verifyPermissionAssignable(permission);

        DataScope scope = (dataScope != null) ? dataScope : DataScope.ALL;

        Optional<RolePermission> existingOpt = findRolePermission(permission.getId());
        if (existingOpt.isPresent()) {
            RolePermission existing = existingOpt.get();
            existing.updateDataScope(scope);
            return existing;
        }

        RolePermission newAssignment = RolePermission.of(this, permission, scope, assignedBy, Instant.now());
        this.permissions.add(newAssignment);
        return newAssignment;
    }

    /**
     * Overloaded method defaulting to DataScope.ALL.
     */
    public RolePermission assignPermission(Permission permission, String assignedBy) {
        return assignPermission(permission, DataScope.ALL, assignedBy);
    }

    /**
     * Revokes a permission assignment by permission ID, preserving domain invariants.
     *
     * @param permissionId the ID of the permission to revoke
     * @return true if a permission assignment was removed, false otherwise
     */
    public boolean revokePermission(Long permissionId) {
        verifyCanBeModified();
        if (permissionId == null) {
            return false;
        }

        return this.permissions.removeIf(rp ->
                rp.getPermission() != null && permissionId.equals(rp.getPermission().getId())
        );
    }

    /**
     * Revokes a permission assignment by Permission entity.
     */
    public boolean revokePermission(Permission permission) {
        if (permission == null) {
            return false;
        }
        return revokePermission(permission.getId());
    }

    /**
     * Synchronizes role permissions to match the target map of Permission -> DataScope.
     *
     * <p>Adds missing permissions, updates modified data scopes, and revokes orphan permissions.
     *
     * @param targetPermissions map of Permission entities to desired DataScopes
     * @param assignedBy the actor performing the sync
     */
    public void syncPermissions(Map<Permission, DataScope> targetPermissions, String assignedBy) {
        verifyCanBeModified();
        if (targetPermissions == null) {
            this.permissions.clear();
            return;
        }

        Set<Long> targetPermissionIds = new HashSet<>();
        for (Map.Entry<Permission, DataScope> entry : targetPermissions.entrySet()) {
            Permission perm = entry.getKey();
            DataScope scope = entry.getValue();
            if (perm != null) {
                targetPermissionIds.add(perm.getId());
                assignPermission(perm, scope, assignedBy);
            }
        }

        this.permissions.removeIf(rp ->
                rp.getPermission() != null && !targetPermissionIds.contains(rp.getPermission().getId())
        );
    }

    /**
     * Checks if this role possesses an active assignment for the given permission code.
     */
    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return this.permissions.stream()
                .filter(RolePermission::isActiveAssignment)
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .anyMatch(p -> permissionCode.equalsIgnoreCase(p.getPermissionCode()));
    }

    /**
     * Returns an unmodifiable view of the assigned permissions set to preserve aggregate encapsulation.
     */
    public Set<RolePermission> getPermissions() {
        return Collections.unmodifiableSet(this.permissions);
    }

    // ==================== Lifecycle Behavior Methods ====================

    /**
     * Updates role display details.
     */
    public void updateDetails(String roleName, String description) {
        verifyCanBeModified();
        if (roleName != null && !roleName.isBlank()) {
            this.roleName = roleName.trim();
        }
        if (description != null && !description.isBlank()) {
            this.description = description.trim();
        }
    }

    /**
     * Deactivates the role.
     */
    public void deactivate() {
        if (this.roleType != RoleType.CUSTOM) {
            throw new IllegalStateException("Cannot deactivate platform-provisioned role: " + this.getId());
        }
        this.status = Status.INACTIVE;
    }

    /**
     * Reactivates the role.
     */
    public void reactivate() {
        if (this.roleType != RoleType.CUSTOM) {
            throw new IllegalStateException("Cannot reactivate platform-provisioned role: " + this.getId());
        }
        this.status = Status.ACTIVE;
    }

    // ==================== Domain Invariant Validations ====================

    private void verifyCanBeModified() {
        if (this.roleType != RoleType.CUSTOM) {
            throw new IllegalStateException("Cannot modify platform-provisioned role: " + this.getRoleCode());
        }
    }

    private void verifyPermissionAssignable(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission to assign must not be null");
        }
        if (!permission.canBeAssigned()) {
            throw new IllegalArgumentException("Cannot assign inactive permission: " + permission.getPermissionCode());
        }
    }

    private Optional<RolePermission> findRolePermission(Long permissionId) {
        if (permissionId == null) {
            return Optional.empty();
        }
        return this.permissions.stream()
                .filter(rp -> rp.getPermission() != null && permissionId.equals(rp.getPermission().getId()))
                .findFirst();
    }

    private static void validateTenantAndRoleCode(Long tenantId, String roleCode) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required for roles");
        }
        validateRoleCode(roleCode);
    }

    private static void validateRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Role code must not be null or blank");
        }
    }

    // ==================== Query Predicates ====================

    public boolean isSystemRole() {
        return this.roleType == RoleType.SYSTEM;
    }

    public boolean isCustomRole() {
        return this.roleType == RoleType.CUSTOM;
    }

    public boolean isTenantRole() {
        return this.roleType == RoleType.TENANT;
    }

    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }

    public boolean isDeactivated() {
        return this.status == Status.INACTIVE;
    }

    public boolean canBeModified() {
        return this.roleType == RoleType.CUSTOM;
    }

    public boolean canBeDeactivated() {
        return this.roleType == RoleType.CUSTOM && this.status == Status.ACTIVE;
    }
}
