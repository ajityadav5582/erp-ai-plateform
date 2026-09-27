package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user department operation violates business rules.
 *
 * @since 1.0.0
 */
public class UserDepartmentOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserDepartmentOperationException(String message) {
        super(message);
    }
}
