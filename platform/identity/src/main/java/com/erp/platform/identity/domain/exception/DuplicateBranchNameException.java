package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a branch name already exists within a tenant.
 *
 * <p>Branch names must be unique within a tenant scope. Maps to
 * HTTP 409 Conflict in the global exception handler.
 *
 * @since 1.0.0
 */
public class DuplicateBranchNameException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateBranchNameException(String message) {
        super(message);
    }

    public DuplicateBranchNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
