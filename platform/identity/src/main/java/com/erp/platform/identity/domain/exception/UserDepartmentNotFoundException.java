package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user department assignment is not found.
 *
 * @since 1.0.0
 */
public class UserDepartmentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserDepartmentNotFoundException(Long userDepartmentId) {
        super("User department assignment not found with ID: " + userDepartmentId);
    }

    public UserDepartmentNotFoundException(Long userId, Long departmentId) {
        super("User department assignment not found for user ID: " + userId + " and department ID: " + departmentId);
    }

    public UserDepartmentNotFoundException(String message) {
        super(message);
    }
}
