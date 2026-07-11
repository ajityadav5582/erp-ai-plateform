package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when attempting to delete a department that has assigned users.
 *
 * @since 1.0.0
 */
public class DepartmentHasAssignedUsersException extends CannotDeleteDepartmentException {

    private static final long serialVersionUID = 1L;

    public DepartmentHasAssignedUsersException(String message) {
        super(message);
    }
}
