package com.erp.platform.identity.domain.exception;

/**
 * Thrown when authentication fails because the supplied credentials are invalid
 * (unknown user, wrong password, or inactive account).
 *
 * <p>For security, the same message is used for "user not found" and "wrong password"
 * so as not to disclose which usernames exist.
 *
 * @since 1.0.0
 */
public class InvalidCredentialsException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
