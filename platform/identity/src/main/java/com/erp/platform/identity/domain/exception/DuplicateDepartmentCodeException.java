package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a department code already exists within a branch.
 *
 * <p>Department codes must be unique within a branch scope. Maps to
 * HTTP 409 Conflict in the global exception handler.
 *
 * @since 1.0.0
 */
public class DuplicateDepartmentCodeException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateDepartmentCodeException(String message) {
        super(message);
    }

    public DuplicateDepartmentCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
