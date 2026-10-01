package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user cannot be activated.
 *
 * <p>Reports {@code 409 CANNOT_ACTIVATE_USER}.
 *
 * @since 1.0.0
 */
public class CannotActivateUserException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public CannotActivateUserException(String message) {
        super(HttpStatus.CONFLICT, "CANNOT_ACTIVATE_USER", message);
    }
}
