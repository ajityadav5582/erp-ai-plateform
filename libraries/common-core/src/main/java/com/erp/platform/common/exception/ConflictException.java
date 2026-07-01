package com.erp.platform.common.exception;

/**
 * Exception thrown when a resource conflict occurs.
 *
 * <p>Typically used for duplicate key violations, optimistic locking failures,
 * or other conflict scenarios.
 *
 * @since 1.0.0
 */
public class ConflictException extends ErpException {

    /**
     * Creates a new conflict exception.
     *
     * @param message the error message
     */
    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    /**
     * Creates a new conflict exception with details.
     *
     * @param message the error message
     * @param details additional error details
     */
    public ConflictException(String message, java.util.Map<String, Object> details) {
        super("CONFLICT", message, details);
    }
}
