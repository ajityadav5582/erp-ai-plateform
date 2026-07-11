package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a branch cannot be found.
 *
 * <p>Maps to HTTP 404 Not Found in the global exception handler.
 *
 * @since 1.0.0
 */
public class BranchNotFoundException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public BranchNotFoundException(String message) {
        super(message);
    }

    public BranchNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
