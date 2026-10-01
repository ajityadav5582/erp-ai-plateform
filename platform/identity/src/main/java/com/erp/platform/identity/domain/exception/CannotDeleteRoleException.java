package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a role cannot be deleted.
 *
 * <p>Reports {@code 409 CANNOT_DELETE_ROLE}.
 *
 * @since 1.0.0
 */
public class CannotDeleteRoleException extends RoleOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeleteRoleException(String message) {
        super(HttpStatus.CONFLICT, "CANNOT_DELETE_ROLE", message);
    }
}
