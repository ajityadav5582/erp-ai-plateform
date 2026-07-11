package com.erp.platform.identity.domain.exception;

/**
 * Base exception for user domain operations.
 *
 * <p>This is the parent class for all domain-specific exceptions
 * related to user lifecycle and business rule violations.
 *
 * @since 1.0.0
 */
public abstract class UserOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected UserOperationException(String message) {
        super(message);
    }

    protected UserOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
