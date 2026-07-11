package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a role with the same code already exists.
 *
 * @since 1.0.0
 */
public class DuplicateRoleCodeException extends RoleOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateRoleCodeException(String message) {
        super(message);
    }
}
