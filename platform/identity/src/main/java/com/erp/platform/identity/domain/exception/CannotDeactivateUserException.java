package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user cannot be deactivated.
 *
 * <p>Reports {@code 409 CANNOT_DEACTIVATE_USER}.
 *
 * @since 1.0.0
 */
public class CannotDeactivateUserException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeactivateUserException(String message) {
        super(HttpStatus.CONFLICT, "CANNOT_DEACTIVATE_USER", message);
    }
}
