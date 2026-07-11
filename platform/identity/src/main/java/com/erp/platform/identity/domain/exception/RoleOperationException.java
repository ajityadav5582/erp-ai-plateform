package com.erp.platform.identity.domain.exception;

/**
 * Base exception for role operation errors.
 *
 * @since 1.0.0
 */
public class RoleOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoleOperationException(String message) {
        super(message);
    }
}
