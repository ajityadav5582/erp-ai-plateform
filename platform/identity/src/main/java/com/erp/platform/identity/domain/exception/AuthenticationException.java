package com.erp.platform.identity.domain.exception;

/**
 * Base exception for authentication and token-related failures in the
 * identity authentication subsystem.
 *
 * <p>These exceptions map to client-facing HTTP errors (typically 401 Unauthorized)
 * and are handled by the global exception handler.
 *
 * @since 1.0.0
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
