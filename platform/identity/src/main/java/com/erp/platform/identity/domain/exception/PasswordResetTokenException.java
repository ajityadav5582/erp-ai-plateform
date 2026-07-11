package com.erp.platform.identity.domain.exception;

/**
 * Thrown when a password reset operation fails because the supplied reset token
 * is missing, invalid, expired, already used, or does not belong to the user.
 *
 * @since 1.0.0
 */
public class PasswordResetTokenException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public PasswordResetTokenException(String message) {
        super(message);
    }
}
