package com.erp.platform.identity.domain.exception;

/**
 * Base exception for branch domain operations.
 *
 * <p>This is the parent class for all domain-specific exceptions
 * related to branch lifecycle and business rule violations.
 *
 * @since 1.0.0
 */
public abstract class BranchOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected BranchOperationException(String message) {
        super(message);
    }

    protected BranchOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
