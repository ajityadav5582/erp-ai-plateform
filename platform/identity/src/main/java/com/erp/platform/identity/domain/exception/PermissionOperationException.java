package com.erp.platform.identity.domain.exception;

/**
 * Base exception for permission operation errors.
 *
 * @since 1.0.0
 */
public class PermissionOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PermissionOperationException(String message) {
        super(message);
    }
}
