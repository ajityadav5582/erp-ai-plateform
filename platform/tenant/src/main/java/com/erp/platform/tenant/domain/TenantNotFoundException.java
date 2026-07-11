package com.erp.platform.tenant.domain;

import java.util.UUID;

/**
 * Exception thrown when a tenant is not found.
 *
 * @since 1.0.0
 */
public class TenantNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TenantNotFoundException(Long id) {
        super("Tenant not found with id: " + id);
    }

    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found with tenantId: " + tenantId);
    }

    public TenantNotFoundException(String tenantCode) {
        super("Tenant not found with tenantCode: " + tenantCode);
    }
}
