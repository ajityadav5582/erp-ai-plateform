package com.erp.platform.tenant.domain.exception;

import com.erp.platform.tenant.domain.TenantStatus;
import java.util.UUID;

/**
 * Exception thrown when attempting to deactivate a tenant that cannot be deactivated.
 *
 * <p>A tenant can only be deactivated if it is in ACTIVE or SUSPENDED status.
 * Pending, expired, deactivated, and archived tenants cannot be deactivated.
 *
 * @since 1.0.0
 */
public class CannotDeactivateTenantException extends TenantOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeactivateTenantException(UUID tenantId, TenantStatus currentStatus) {
        super("Cannot deactivate tenant with id: " + tenantId + ". Current status: " + currentStatus);
    }

    public CannotDeactivateTenantException(String tenantCode, TenantStatus currentStatus) {
        super("Cannot deactivate tenant with code: " + tenantCode + ". Current status: " + currentStatus);
    }
}
