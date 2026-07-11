package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a department cannot be moved to a different branch or parent.
 *
 * @since 1.0.0
 */
public class CannotMoveDepartmentException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public CannotMoveDepartmentException(String message) {
        super(message);
    }
}
