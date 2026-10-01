package com.erp.platform.tenant.domain.exception;

import java.util.UUID;

/**
 * Base exception for tenant domain operations.
 *
 * <p>This is the parent class for all domain-specific exceptions
 * related to tenant lifecycle and business rule violations.
 *
 * @since 1.0.0
 */
public abstract class TenantOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected TenantOperationException(String message) {
        super(message);
    }

    protected TenantOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
