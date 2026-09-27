package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

/**
 * Business resource (module) master data aggregate.
 *
 * <p>Represents a business module/domain in the ERP platform (e.g. PRODUCT,
 * SALES, BRANCH, DEPARTMENT, USER). Resources are used to categorize
 * permissions and organize the platform's functional areas.
 *
 * <p>Resources are master data shared across tenants and are therefore
 * not tenant-scoped.
 *
 * @since 1.0.0
 */
@AttributeOverride(
        name = "id",
        column = @Column(name = "id", columnDefinition = "BIGINT")
)
@Entity
@Table(name = "resources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class ResourceMaster extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Unique resource code (e.g. PRODUCT, SALES, BRANCH).
     * Matches the {@link Resource} enum name where applicable.
     */
    @Column(name = "resource_code", nullable = false, length = 50, unique = true)
    private String resourceCode;

    /**
     * Human-readable name of the resource.
     */
    @Column(name = "resource_name", nullable = false, length = 100)
    private String resourceName;

    /**
     * Module/domain this resource belongs to (e.g. IDENTITY, SALES).
     * Mirrors the top-level grouping of the {@link Resource} enum.
     */
    @Column(name = "module", length = 50)
    private String module;

    /**
     * Detailed description of the resource's purpose.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Current status of the resource.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ResourceStatus status;

    /**
     * Indicates if this is a system-level resource that cannot be deleted or modified by tenants.
     * System resources are pre-defined platform modules (e.g. IDENTITY, PLATFORM).
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    // ==================== Domain Methods ====================

    public void activate() {
        if (this.status == ResourceStatus.ACTIVE) {
            throw new IllegalStateException("Resource is already active");
        }
        this.status = ResourceStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status != ResourceStatus.ACTIVE) {
            throw new IllegalStateException("Only active resources can be deactivated");
        }
        this.status = ResourceStatus.INACTIVE;
    }

    // ==================== Factory Methods ====================

    public static ResourceMaster create(String resourceCode, String resourceName, String module, String description, ResourceStatus status, Boolean isSystem) {
        validateResourceCode(resourceCode);
        validateResourceName(resourceName);

        return ResourceMaster.builder()
                .resourceCode(resourceCode.trim().toUpperCase())
                .resourceName(resourceName.trim())
                .module(module != null ? module.trim().toUpperCase() : null)
                .description(description != null ? description.trim() : null)
                .status(status != null ? status : ResourceStatus.ACTIVE)
                .isSystem(isSystem != null ? isSystem : false)
                .build();
    }

    // ==================== Validation Methods ====================

    private static void validateResourceCode(String resourceCode) {
        if (resourceCode == null || resourceCode.isBlank()) {
            throw new IllegalArgumentException("Resource code must not be blank");
        }
        if (resourceCode.length() > 50) {
            throw new IllegalArgumentException("Resource code must not exceed 50 characters");
        }
    }

    private static void validateResourceName(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("Resource name must not be blank");
        }
        if (resourceName.length() > 100) {
            throw new IllegalArgumentException("Resource name must not exceed 100 characters");
        }
    }
}
