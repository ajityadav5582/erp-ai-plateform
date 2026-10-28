package com.erp.platform.identity.domain.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a permission is not found.
 *
 * <p>Reports {@code 404 PERMISSION_NOT_FOUND}.
 *
 * <p>Note: this type intentionally extends {@link BaseApiException} directly rather
 * than {@code PermissionOperationException}. The other permission exceptions
 * ({@code CannotDeletePermissionException}, {@code DuplicatePermissionCodeException})
 * are not currently mapped to a specific HTTP response and fall through to the
 * generic 500 handler. Extending the operation base here would drag them into a
 * status they never had, so this class stays independent of that hierarchy.
 *
 * @since 1.0.0
 */
public class PermissionNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public PermissionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "PERMISSION_NOT_FOUND", message);
    }

    public static PermissionNotFoundException byId(Long id) {
        return new PermissionNotFoundException("Permission not found with id: " + id);
    }

    public static PermissionNotFoundException byPermissionId(Long permissionId) {
        return new PermissionNotFoundException("Permission not found with permissionId: " + permissionId);
    }

    public static PermissionNotFoundException byPermissionCode(String permissionCode) {
        return new PermissionNotFoundException("Permission not found with permissionCode: " + permissionCode);
    }
}
