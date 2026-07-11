package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a permission is not found.
 *
 * @since 1.0.0
 */
public class PermissionNotFoundException extends PermissionOperationException {

    private static final long serialVersionUID = 1L;

    public PermissionNotFoundException(String message) {
        super(message);
    }

    public static PermissionNotFoundException byId(Long id) {
        return new PermissionNotFoundException("Permission not found with id: " + id);
    }

    public static PermissionNotFoundException byPermissionId(java.util.UUID permissionId) {
        return new PermissionNotFoundException("Permission not found with permissionId: " + permissionId);
    }

    public static PermissionNotFoundException byPermissionCode(String permissionCode) {
        return new PermissionNotFoundException("Permission not found with permissionCode: " + permissionCode);
    }
}
