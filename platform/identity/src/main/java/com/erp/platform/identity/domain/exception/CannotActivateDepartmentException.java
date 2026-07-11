package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a department cannot be activated.
 *
 * @since 1.0.0
 */
public class CannotActivateDepartmentException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public CannotActivateDepartmentException(String message) {
        super(message);
    }
}
