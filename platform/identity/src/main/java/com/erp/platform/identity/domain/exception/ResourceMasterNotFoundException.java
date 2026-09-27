package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a resource cannot be located.
 *
 * @since 1.0.0
 */
public class ResourceMasterNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new resource not found exception.
     *
     * @param message the detail message
     */
    public ResourceMasterNotFoundException(String message) {
        super(message);
    }
}
