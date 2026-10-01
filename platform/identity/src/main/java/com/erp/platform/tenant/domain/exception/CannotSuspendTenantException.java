package com.erp.platform.tenant.domain.exception;

import com.erp.platform.tenant.domain.TenantStatus;
import java.util.UUID;

/**
 * Exception thrown when attempting to suspend a tenant that cannot be suspended.
 *
 * <p>A tenant can only be suspended if it is in ACTIVE status.
 * Pending, suspended, expired, deactivated, and archived tenants cannot be suspended.
 *
 * @since 1.0.0
 */
public class CannotSuspendTenantException extends TenantOperationException {

    private static final long serialVersionUID = 1L;

    public CannotSuspendTenantException(UUID tenantId, TenantStatus currentStatus) {
        super("Cannot suspend tenant with id: " + tenantId + ". Current status: " + currentStatus);
    }

    public CannotSuspendTenantException(String tenantCode, TenantStatus currentStatus) {
        super("Cannot suspend tenant with code: " + tenantCode + ". Current status: " + currentStatus);
    }
}
