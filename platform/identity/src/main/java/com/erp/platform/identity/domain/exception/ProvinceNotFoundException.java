package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a province cannot be located.
 *
 * @since 1.0.0
 */
public class ProvinceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new province not found exception.
     *
     * @param message the detail message
     */
    public ProvinceNotFoundException(String message) {
        super(message);
    }
}
