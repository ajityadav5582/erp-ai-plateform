package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user cannot be activated.
 *
 * @since 1.0.0
 */
public class CannotActivateUserException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public CannotActivateUserException(String message) {
        super(message);
    }
}
