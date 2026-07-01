package com.erp.platform.common.tenancy;

/**
 * Marker interface for entities that are tenant-aware.
 *
 * <p>Entities implementing this interface are automatically
 * filtered by tenant ID in queries.
 *
 * @since 1.0.0
 */
public interface TenantAware {

    /**
     * Gets the tenant ID.
     *
     * @return the tenant ID
     */
    String getTenantId();

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID
     */
    void setTenantId(String tenantId);
}
