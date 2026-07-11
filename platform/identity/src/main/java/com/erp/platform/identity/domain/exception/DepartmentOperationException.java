package com.erp.platform.identity.domain.exception;

/**
 * Base exception for department domain operations.
 *
 * <p>This is the parent class for all domain-specific exceptions
 * related to department lifecycle and business rule violations.
 *
 * @since 1.0.0
 */
public abstract class DepartmentOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected DepartmentOperationException(String message) {
        super(message);
    }

    protected DepartmentOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
