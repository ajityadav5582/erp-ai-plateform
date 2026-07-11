package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user with the same email already exists within a tenant.
 *
 * @since 1.0.0
 */
public class DuplicateEmailException extends UserOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateEmailException(String message) {
        super(message);
    }
}
