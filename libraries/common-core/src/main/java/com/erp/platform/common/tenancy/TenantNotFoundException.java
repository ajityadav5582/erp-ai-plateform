package com.erp.platform.common.tenancy;

/**
 * Exception thrown when a tenant is not found.
 *
 * @since 1.0.0
 */
public class TenantNotFoundException extends RuntimeException {

    /**
     * Creates a new tenant not found exception.
     *
     * @param tenantId the tenant ID that was not found
     */
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }
}
