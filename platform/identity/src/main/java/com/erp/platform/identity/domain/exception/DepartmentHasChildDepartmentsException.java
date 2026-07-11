package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when attempting to delete a department that has child departments.
 *
 * @since 1.0.0
 */
public class DepartmentHasChildDepartmentsException extends CannotDeleteDepartmentException {

    private static final long serialVersionUID = 1L;

    public DepartmentHasChildDepartmentsException(String message) {
        super(message);
    }
}
