package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an email address is already registered.
 *
 * <p>Reports {@code 409 DUPLICATE_EMAIL}.
 *
 * @since 1.0.0
 */
public class DuplicateEmailException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateEmailException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", message);
    }
}
