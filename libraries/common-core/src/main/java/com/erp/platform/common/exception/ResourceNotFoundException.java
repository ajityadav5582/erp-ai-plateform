package com.erp.platform.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 *
 * @since 1.0.0
 */
public class ResourceNotFoundException extends ErpException {

    /**
     * Creates a new resource not found exception.
     *
     * @param resourceType the type of resource (e.g., "Invoice", "Customer")
     * @param id the resource identifier
     */
    public ResourceNotFoundException(String resourceType, Object id) {
        super("NOT_FOUND",
            String.format("%s with id '%s' not found", resourceType, id));
    }

    /**
     * Creates a new resource not found exception with custom message.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
