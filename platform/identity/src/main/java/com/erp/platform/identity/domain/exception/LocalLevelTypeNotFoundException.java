package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a local level type cannot be located.
 *
 * @since 2.0.0
 */
public class LocalLevelTypeNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new local level type not found exception.
     *
     * @param message the detail message
     */
    public LocalLevelTypeNotFoundException(String message) {
        super(message);
    }
}
