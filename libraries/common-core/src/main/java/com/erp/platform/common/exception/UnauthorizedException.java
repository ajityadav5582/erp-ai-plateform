package com.erp.platform.common.exception;

/**
 * Exception thrown when authentication fails or user is not authenticated.
 *
 * @since 1.0.0
 */
public class UnauthorizedException extends ErpException {

    /**
     * Creates a new unauthorized exception.
     *
     * @param message the error message
     */
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
