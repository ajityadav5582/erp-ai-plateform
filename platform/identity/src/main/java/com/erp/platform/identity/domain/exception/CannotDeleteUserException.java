package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user cannot be deleted.
 *
 * <p>Reports {@code 409 CANNOT_DELETE_USER}.
 *
 * @since 1.0.0
 */
public class CannotDeleteUserException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeleteUserException(String message) {
        super(HttpStatus.CONFLICT, "CANNOT_DELETE_USER", message);
    }
}
