package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a permission cannot be deleted.
 *
 * @since 1.0.0
 */
public class CannotDeletePermissionException extends PermissionOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeletePermissionException(String message) {
        super(message);
    }

    public static CannotDeletePermissionException permissionInUse(String permissionCode) {
        return new CannotDeletePermissionException(
            "Cannot delete permission '" + permissionCode + "' because it is assigned to one or more roles"
        );
    }
}
