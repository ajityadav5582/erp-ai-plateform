package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user cannot be deleted.
 *
 * @since 1.0.0
 */
public class CannotDeleteUserException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeleteUserException(String message) {
        super(message);
    }
}
