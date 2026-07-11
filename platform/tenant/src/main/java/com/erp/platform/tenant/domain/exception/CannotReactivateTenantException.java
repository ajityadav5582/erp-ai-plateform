package com.erp.platform.tenant.domain.exception;

import com.erp.platform.tenant.domain.TenantStatus;
import java.util.UUID;

/**
 * Exception thrown when attempting to reactivate a tenant that cannot be reactivated.
 *
 * <p>A tenant can only be reactivated if it is in SUSPENDED status.
 * Pending, active, expired, deactivated, and archived tenants cannot be reactivated.
 *
 * @since 1.0.0
 */
public class CannotReactivateTenantException extends TenantOperationException {

    private static final long serialVersionUID = 1L;

    public CannotReactivateTenantException(UUID tenantId, TenantStatus currentStatus) {
        super("Cannot reactivate tenant with id: " + tenantId + ". Current status: " + currentStatus);
    }

    public CannotReactivateTenantException(String tenantCode, TenantStatus currentStatus) {
        super("Cannot reactivate tenant with code: " + tenantCode + ". Current status: " + currentStatus);
    }
}
