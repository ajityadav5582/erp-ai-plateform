package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user cannot be deactivated.
 *
 * @since 1.0.0
 */
public class CannotDeactivateUserException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeactivateUserException(String message) {
        super(message);
    }
}
