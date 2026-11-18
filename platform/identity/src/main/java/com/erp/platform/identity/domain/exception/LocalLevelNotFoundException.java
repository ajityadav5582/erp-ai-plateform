package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a local level cannot be located.
 *
 * @since 2.0.0
 */
public class LocalLevelNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new local level not found exception.
     *
     * @param message the detail message
     */
    public LocalLevelNotFoundException(String message) {
        super(message);
    }
}
