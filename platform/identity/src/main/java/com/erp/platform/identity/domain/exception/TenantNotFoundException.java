package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a tenant is not found.
 *
 * <p>Reports {@code 404 TENANT_NOT_FOUND}.
 *
 * @since 1.0.0
 */
public class TenantNotFoundException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public TenantNotFoundException(Long tenantId) {
        this("Tenant not found with ID: " + tenantId);
    }

    public TenantNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", message);
    }
}
