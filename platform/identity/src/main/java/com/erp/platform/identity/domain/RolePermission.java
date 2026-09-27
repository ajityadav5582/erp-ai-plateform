package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Assignment row joining a role to a permission. */
@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_role_permission", columnNames = {"role_id", "permission_id"}),
        indexes = {
                @Index(name = "idx_role_perm_role", columnList = "role_id"),
                @Index(name = "idx_role_perm_permission", columnList = "permission_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class RolePermission {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false, columnDefinition = "timestamp")
    private LocalDateTime assignedAt;

    @Builder.Default
    @org.hibernate.annotations.ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Transient
    @Builder.Default
    private DataScope dataScope = DataScope.ALL;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static RolePermission of(Role role, Permission permission, DataScope dataScope,
                                    String assignedBy, Instant assignedAt) {
        if (role == null || permission == null) {
            throw new IllegalArgumentException("Role and Permission must not be null");
        }
        if (assignedBy == null || assignedBy.isBlank()) {
            throw new IllegalArgumentException("assignedBy must not be blank");
        }
        return RolePermission.builder()
                .role(role)
                .permission(permission)
                .assignedBy(assignedBy.trim())
                .assignedAt(toLocalDateTime(assignedAt))
                .active(true)
                .dataScope(dataScope != null ? dataScope : DataScope.ALL)
                .build();
    }

    /** Legacy factory retained for callers that provide IDs and tenant context. */
    public static RolePermission assign(Long tenantId, Long roleId, Long permissionId,
                                        String assignedBy, Instant assignedAt) {
        Role role = Role.builder().build();
        role.setId(roleId);
        Permission permission = Permission.builder().build();
        permission.setId(permissionId);
        return of(role, permission, DataScope.ALL,
                assignedBy != null && !assignedBy.isBlank() ? assignedBy : "system", assignedAt);
    }

    public void updateDataScope(DataScope newDataScope) {
        if (newDataScope != null) dataScope = newDataScope;
    }

    public void deactivate(Instant ignoredTimestamp) {
        active = false;
    }

    public void reactivate() {
        active = true;
    }

    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }

    public Long getPermissionId() {
        return permission != null ? permission.getId() : null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    void setRoleInternal(Role role) {
        this.role = role;
    }

    public boolean isActiveAssignment() {
        return active;
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? LocalDateTime.now(ZoneOffset.UTC) : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
