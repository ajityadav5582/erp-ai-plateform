package com.erp.platform.common.exception;

/**
 * Exception thrown when the authenticated user is not authorized to access a resource.
 *
 * @since 1.0.0
 */
public class ForbiddenException extends ErpException {

    /**
     * Creates a new forbidden exception.
     *
     * @param message the error message
     */
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
