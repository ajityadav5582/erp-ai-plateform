package com.erp.platform.tenant.domain.exception;

import com.erp.platform.tenant.domain.TenantStatus;
import java.util.UUID;

/**
 * Exception thrown when attempting to activate a tenant that cannot be activated.
 *
 * <p>A tenant can only be activated if it is in PENDING, TRIAL, or EXPIRED status.
 * Active, suspended, deactivated, and archived tenants cannot be activated.
 *
 * @since 1.0.0
 */
public class CannotActivateTenantException extends TenantOperationException {

    private static final long serialVersionUID = 1L;

    public CannotActivateTenantException(UUID tenantId, TenantStatus currentStatus) {
        super("Cannot activate tenant with id: " + tenantId + ". Current status: " + currentStatus);
    }

    public CannotActivateTenantException(String tenantCode, TenantStatus currentStatus) {
        super("Cannot activate tenant with code: " + tenantCode + ". Current status: " + currentStatus);
    }
}
