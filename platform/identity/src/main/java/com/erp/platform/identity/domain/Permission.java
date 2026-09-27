package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A permission identified by a resource/action pair. */
@Entity
@Table(
        name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissions_code", columnNames = "code"),
        indexes = @Index(name = "idx_permissions_resource_action", columnList = "resource, action")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 150)
    private String permissionCode;

    @Column(name = "name", nullable = false, length = 200)
    private String permissionName;

    @Column(name = "resource", nullable = false, length = 100)
    private String resourceCode;

    @Column(name = "action", nullable = false, length = 50)
    private String actionCode;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PermissionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Permission create(
            Long id,
            ResourceMaster resource,
            ActionEntity action,
            String permissionName,
            String description) {
        if (resource == null || action == null) {
            throw new IllegalArgumentException("Resource and action are required to create a Permission");
        }
        String code = generatePermissionCode(resource, action);
        Permission permission = Permission.builder()
                .permissionCode(code)
                .permissionName(permissionName != null && !permissionName.isBlank() ? permissionName.trim() : code)
                .resourceCode(resource.getResourceCode())
                .actionCode(action.getActionCode())
                .description(description != null ? description.trim() : null)
                .status(PermissionStatus.ACTIVE)
                .build();
        if (id != null) permission.setId(id);
        return permission;
    }

    public static String generatePermissionCode(ResourceMaster resource, ActionEntity action) {
        if (resource == null || action == null) {
            throw new IllegalArgumentException("Resource and Action entities are required");
        }
        return generatePermissionCode(resource.getResourceCode(), action.getActionCode());
    }

    public static String generatePermissionCode(String resourceCode, String actionCode) {
        if (resourceCode == null || resourceCode.isBlank() || actionCode == null || actionCode.isBlank()) {
            throw new IllegalArgumentException("Resource code and action code must not be null or blank");
        }
        return resourceCode.trim().toUpperCase() + "_" + actionCode.trim().toUpperCase();
    }

    public static String generatePermissionCode(Resource resource, Action action) {
        if (resource == null || action == null) {
            throw new IllegalArgumentException("Resource and action enums are required");
        }
        return resource.name() + "_" + action.getCode();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void deactivate(java.time.Instant ignoredTimestamp) {
        this.status = PermissionStatus.INACTIVE;
    }

    public void reactivate() {
        this.status = PermissionStatus.ACTIVE;
    }

    public void updateDescription(String description) {
        if (description != null && !description.isBlank()) this.description = description.trim();
    }

    public boolean isActive() {
        return status == PermissionStatus.ACTIVE;
    }

    public boolean isInactive() {
        return status == PermissionStatus.INACTIVE;
    }

    public boolean canBeAssigned() {
        return isActive();
    }

    public boolean canBeDeactivated() {
        return isActive();
    }

    public boolean canBeReactivated() {
        return isInactive();
    }
}
