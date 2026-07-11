package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a department cannot be deleted.
 *
 * @since 1.0.0
 */
public class CannotDeleteDepartmentException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeleteDepartmentException(String message) {
        super(message);
    }
}
