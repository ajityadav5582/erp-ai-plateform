package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a department cannot be deactivated.
 *
 * @since 1.0.0
 */
public class CannotDeactivateDepartmentException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeactivateDepartmentException(String message) {
        super(message);
    }
}
