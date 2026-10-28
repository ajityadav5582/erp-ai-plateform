package com.erp.platform.identity.domain.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the caller lacks the required permission.
 *
 * <p>Reports {@code 403 ACCESS_DENIED}.
 *
 * @since 1.0.0
 */
public class AccessDeniedException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCESS_DENIED", message);
    }

    /**
     * Creates an AccessDeniedException for missing permission.
     *
     * @param requiredPermission the permission that was required
     * @return the exception
     * @since 1.0.0
     */
    public static AccessDeniedException forPermission(String requiredPermission) {
        return new AccessDeniedException("Required permission: " + requiredPermission);
    }

    /**
     * Creates an AccessDeniedException for missing permissions.
     *
     * @param requiredPermissions the permissions that were required
     * @return the exception
     * @since 1.0.0
     */
    public static AccessDeniedException forAnyPermission(String... requiredPermissions) {
        return new AccessDeniedException("Required any of permissions: " + String.join(", ", requiredPermissions));
    }

    /**
     * Creates an AccessDeniedException for missing all permissions.
     *
     * @param requiredPermissions the permissions that were required
     * @return the exception
     * @since 1.0.0
     */
    public static AccessDeniedException forAllPermissions(String... requiredPermissions) {
        return new AccessDeniedException("Required all permissions: " + String.join(", ", requiredPermissions));
    }
}
