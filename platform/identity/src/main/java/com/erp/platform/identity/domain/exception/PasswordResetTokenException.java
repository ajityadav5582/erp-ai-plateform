package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a password reset token is invalid or expired.
 *
 * <p>Reports {@code 400 PASSWORD_RESET_TOKEN_ERROR}.
 *
 * @since 1.0.0
 */
public class PasswordResetTokenException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public PasswordResetTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_ERROR", message);
    }
}
