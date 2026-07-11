package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a permission with the same code already exists.
 *
 * @since 1.0.0
 */
public class DuplicatePermissionCodeException extends PermissionOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicatePermissionCodeException(String permissionCode) {
        super("Permission with code '" + permissionCode + "' already exists");
    }
}
