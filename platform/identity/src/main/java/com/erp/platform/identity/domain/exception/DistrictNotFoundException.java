package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a district cannot be found.
 */
public class DistrictNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new district not found exception.
     *
     * @param message the detail message
     */
    public DistrictNotFoundException(String message) {
        super(message);
    }
}
