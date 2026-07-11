package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a role cannot be deleted.
 *
 * @since 1.0.0
 */
public class CannotDeleteRoleException extends RoleOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeleteRoleException(String message) {
        super(message);
    }
}
