package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a role code is already in use.
 *
 * <p>Reports {@code 409 DUPLICATE_ROLE_CODE}.
 *
 * @since 1.0.0
 */
public class DuplicateRoleCodeException extends RoleOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateRoleCodeException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_ROLE_CODE", message);
    }
}
