package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Association entity allowing a user to hold multiple roles. */
@Entity
@Table(
        name = "user_roles",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_role", columnNames = {"user_id", "role_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private Boolean isPrimaryRole = false;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false, columnDefinition = "timestamp")
    private LocalDateTime assignedAt;

    @Column(name = "expires_at", columnDefinition = "timestamp")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "active", nullable = false)
    @org.hibernate.annotations.ColumnDefault("true")
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }

    public static UserRole assign(Long userId, Long roleId, Long tenantId, String assignedBy,
                                  Instant assignedAt, Instant expiresAt, Boolean isPrimaryRole) {
        return assign(userId, roleId, tenantId, assignedBy, toLocalDateTime(assignedAt),
                toLocalDateTime(expiresAt), isPrimaryRole);
    }

    public static UserRole assign(Long userId, Long roleId, Long tenantId, String assignedBy,
                                  LocalDateTime assignedAt, LocalDateTime expiresAt, Boolean isPrimaryRole) {
        if (userId == null || roleId == null || tenantId == null) {
            throw new IllegalArgumentException("User, role, and tenant IDs are required");
        }
        if (assignedAt == null) throw new IllegalArgumentException("Assigned at is required");
        return UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedBy(assignedBy)
                .assignedAt(assignedAt)
                .expiresAt(expiresAt)
                .isPrimaryRole(Boolean.TRUE.equals(isPrimaryRole))
                .active(true)
                .build();
    }

    public void revoke(Instant ignoredRevokedAt, String ignoredRevokedBy, String ignoredReason) {
        active = false;
        isPrimaryRole = false;
    }

    public void reactivate(String assignedBy, LocalDateTime assignedAt, LocalDateTime expiresAt, Boolean primary) {
        active = true;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.isPrimaryRole = Boolean.TRUE.equals(primary);
    }

    public void extendExpiration(Instant newExpiresAt) {
        if (!active) throw new IllegalStateException("Cannot extend an inactive assignment: " + id);
        expiresAt = toLocalDateTime(newExpiresAt);
    }

    public void removeExpiration() {
        if (!active) throw new IllegalStateException("Cannot update an inactive assignment: " + id);
        expiresAt = null;
    }

    public void setAsPrimary() {
        if (!active) throw new IllegalStateException("Cannot set an inactive assignment as primary: " + id);
        isPrimaryRole = true;
    }

    public void removePrimaryDesignation() {
        if (!active) throw new IllegalStateException("Cannot update an inactive assignment: " + id);
        isPrimaryRole = false;
    }

    public boolean isActive() {
        return active && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    public boolean isRevoked() {
        return !active;
    }

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isPrimaryRole() {
        return Boolean.TRUE.equals(isPrimaryRole);
    }

    public boolean canBeRevoked() {
        return isActive();
    }

    public boolean canBeExtended() {
        return isActive() && expiresAt != null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
