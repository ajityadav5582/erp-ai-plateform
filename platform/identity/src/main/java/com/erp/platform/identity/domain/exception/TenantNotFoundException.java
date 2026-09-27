package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a tenant cannot be found.
 *
 * <p>This is a business-level exception that maps to a 404 Not Found response.
 *
 * @since 1.0.0
 */
public class TenantNotFoundException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public TenantNotFoundException(Long tenantId) {
        super("Tenant not found with ID: " + tenantId);
    }

    public TenantNotFoundException(String message) {
        super(message);
    }
}
