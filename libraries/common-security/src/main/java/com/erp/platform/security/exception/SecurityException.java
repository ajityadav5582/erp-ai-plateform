package com.erp.platform.security.exception;

/**
 * Base exception for security-related errors.
 *
 * <p>This is the root exception for all security-related
 * exceptions in the platform.
 *
 * @since 1.0.0
 */
public class SecurityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new security exception with the specified message.
     *
     * @param message the error message
     */
    public SecurityException(String message) {
        super(message);
    }

    /**
     * Constructs a new security exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause the cause
     */
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
