package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when supplied credentials are invalid.
 *
 * <p>Reports {@code 401 INVALID_CREDENTIALS}.
 *
 * @since 1.0.0
 */
public class InvalidCredentialsException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }
}
