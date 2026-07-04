package com.erp.platform.security.authentication;

import java.util.Collection;
import java.util.Set;

/**
 * Abstraction for an authenticated user.
 *
 * <p>This interface provides a platform-agnostic representation
 * of an authenticated user, independent of the authentication
 * mechanism (JWT, Keycloak, OAuth2, etc.).
 *
 * <p>Implementations will be provided by platform services.
 *
 * @since 1.0.0
 */
public interface AuthenticatedUser {

    /**
     * Returns the unique identifier of the user.
     *
     * @return the user ID
     */
    String getUserId();

    /**
     * Returns the username of the user.
     *
     * @return the username
     */
    String getUsername();

    /**
     * Returns the email of the user.
     *
     * @return the email
     */
    String getEmail();

    /**
     * Returns the full name of the user.
     *
     * @return the full name
     */
    String getFullName();

    /**
     * Returns the tenant ID associated with the user.
     *
     * @return the tenant ID
     */
    Long getTenantId();

    /**
     * Returns the roles assigned to the user.
     *
     * @return the roles
     */
    Collection<String> getRoles();

    /**
     * Returns the permissions assigned to the user.
     *
     * @return the permissions
     */
    Collection<String> getPermissions();

    /**
     * Checks if the user has a specific role.
     *
     * @param role the role to check
     * @return true if the user has the role
     */
    boolean hasRole(String role);

    /**
     * Checks if the user has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Checks if the user has any of the specified roles.
     *
     * @param roles the roles to check
     * @return true if the user has any of the roles
     */
    boolean hasAnyRole(String... roles);

    /**
     * Checks if the user has all of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has all permissions
     */
    boolean hasAllPermissions(String... permissions);
}
