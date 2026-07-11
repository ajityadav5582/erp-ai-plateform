package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a branch code already exists within a tenant.
 *
 * <p>Branch codes must be unique within a tenant scope. Maps to
 * HTTP 409 Conflict in the global exception handler.
 *
 * @since 1.0.0
 */
public class DuplicateBranchCodeException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateBranchCodeException(String message) {
        super(message);
    }

    public DuplicateBranchCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
