package com.erp.platform.identity.domain.exception;

/**
 * Base exception for user-branch domain operations.
 *
 * <p>This is the parent class for all domain-specific exceptions
 * related to user-branch assignment lifecycle and business rule violations.
 *
 * @since 1.0.0
 */
public abstract class UserBranchOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected UserBranchOperationException(String message) {
        super(message);
    }

    protected UserBranchOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
