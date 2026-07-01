package com.erp.platform.security.utils;

import com.erp.platform.security.context.SecurityContext;
import com.erp.platform.security.exception.SecurityException;

/**
 * Security utility methods.
 *
 * <p>This class provides utility methods for common security operations.
 *
 * @since 1.0.0
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Ensures that the current user is authenticated.
     *
     * @param securityContext the security context
     * @throws SecurityException if the user is not authenticated
     */
    public static void requireAuthentication(SecurityContext securityContext) {
        if (securityContext == null || !securityContext.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }
    }

    /**
     * Ensures that the current user has the specified permission.
     *
     * @param securityContext the security context
     * @param permission the permission to check
     * @throws SecurityException if the user does not have the permission
     */
    public static void requirePermission(SecurityContext securityContext, String permission) {
        requireAuthentication(securityContext);
        if (!securityContext.hasPermission(permission)) {
            throw new SecurityException(
                "User does not have permission: " + permission
            );
        }
    }

    /**
     * Ensures that the current user has the specified role.
     *
     * @param securityContext the security context
     * @param role the role to check
     * @throws SecurityException if the user does not have the role
     */
    public static void requireRole(SecurityContext securityContext, String role) {
        requireAuthentication(securityContext);
        if (!securityContext.hasRole(role)) {
            throw new SecurityException(
                "User does not have role: " + role
            );
        }
    }
}
