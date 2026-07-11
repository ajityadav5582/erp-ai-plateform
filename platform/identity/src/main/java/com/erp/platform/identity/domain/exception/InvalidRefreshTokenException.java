package com.erp.platform.identity.domain.exception;

/**
 * Thrown when a refresh token is invalid, expired, revoked, or not found.
 *
 * @since 1.0.0
 */
public class InvalidRefreshTokenException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
