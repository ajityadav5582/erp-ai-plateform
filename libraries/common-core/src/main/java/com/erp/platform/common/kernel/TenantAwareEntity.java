package com.erp.platform.common.kernel;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;

/**
 * Tenant-aware entity providing multi-tenant data isolation.
 *
 * <p>All business entities in a multi-tenant system should extend this class
 * to ensure data is always associated with a tenant.
 *
 * @param <T> the type of the entity identifier
 */
@MappedSuperclass
public abstract class TenantAwareEntity<T extends Serializable> extends BaseEntity<T> {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /**
     * Returns the tenant identifier.
     *
     * @return the tenant ID
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant identifier.
     *
     * <p>This method should only be called by the persistence layer
     * or during entity creation. The tenant ID should be set from
     * the security context, never from user input.
     *
     * @param tenantId the tenant ID
     */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Checks if the entity belongs to the specified tenant.
     *
     * @param tenantId the tenant ID to check
     * @return true if the entity belongs to the tenant, false otherwise
     */
    public boolean belongsToTenant(Long tenantId) {
        return this.tenantId != null && this.tenantId.equals(tenantId);
    }
}
