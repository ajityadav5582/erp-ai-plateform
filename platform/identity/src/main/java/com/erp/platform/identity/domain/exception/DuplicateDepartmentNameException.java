package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a department name already exists within a branch.
 *
 * <p>Department names must be unique within a branch scope. Maps to
 * HTTP 409 Conflict in the global exception handler.
 *
 * @since 1.0.0
 */
public class DuplicateDepartmentNameException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateDepartmentNameException(String message) {
        super(message);
    }

    public DuplicateDepartmentNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
