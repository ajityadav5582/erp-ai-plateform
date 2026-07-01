package com.erp.platform.security.context;

import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.security.tenant.TenantPrincipal;

/**
 * Security context abstraction.
 *
 * <p>This interface provides access to the current security context,
 * including the authenticated user and tenant information.
 *
 * <p>Implementations will be provided by platform services.
 *
 * @since 1.0.0
 */
public interface SecurityContext {

    /**
     * Returns the current authenticated user.
     *
     * @return the authenticated user, or null if not authenticated
     */
    AuthenticatedUser getCurrentUser();

    /**
     * Returns the current tenant principal.
     *
     * @return the tenant principal, or null if not available
     */
    TenantPrincipal getTenantPrincipal();

    /**
     * Checks if a user is currently authenticated.
     *
     * @return true if a user is authenticated
     */
    boolean isAuthenticated();

    /**
     * Checks if the current user has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Checks if the current user has any of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has any of the permissions
     */
    boolean hasAnyPermission(String... permissions);

    /**
     * Checks if the current user has all of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has all permissions
     */
    boolean hasAllPermissions(String... permissions);

    /**
     * Checks if the current user has a specific role.
     *
     * @param role the role to check
     * @return true if the user has the role
     */
    boolean hasRole(String role);

    /**
     * Checks if the current user has any of the specified roles.
     *
     * @param roles the roles to check
     * @return true if the user has any of the roles
     */
    boolean hasAnyRole(String... roles);
}
