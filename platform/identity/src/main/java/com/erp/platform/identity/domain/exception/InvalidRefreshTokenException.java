package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token is invalid or expired.
 *
 * <p>Reports {@code 401 INVALID_REFRESH_TOKEN}.
 *
 * @since 1.0.0
 */
public class InvalidRefreshTokenException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", message);
    }
}
