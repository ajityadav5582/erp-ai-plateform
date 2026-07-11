package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a department cannot be found.
 *
 * <p>Maps to HTTP 404 Not Found in the global exception handler.
 *
 * @since 1.0.0
 */
public class DepartmentNotFoundException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public DepartmentNotFoundException(String message) {
        super(message);
    }

    public DepartmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
