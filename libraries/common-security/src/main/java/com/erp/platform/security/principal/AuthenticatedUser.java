package com.erp.platform.security.principal;

import java.io.Serializable;
import java.util.Set;

/**
 * Authenticated user abstraction.
 *
 * <p>Represents the currently authenticated user in the system.
 * This is a security abstraction that should be populated by the
 * authentication mechanism (e.g., Keycloak, OAuth2).
 *
 * @since 1.0.0
 */
public interface AuthenticatedUser extends Serializable {

    /**
     * Returns the user identifier.
     *
     * @return the user ID
     */
    String getUserId();

    /**
     * Returns the user's email address.
     *
     * @return the email
     */
    String getEmail();

    /**
     * Returns the user's full name.
     *
     * @return the full name
     */
    String getFullName();

    /**
     * Returns the tenant identifier.
     *
     * @return the tenant ID
     */
    Long getTenantId();

    /**
     * Returns the user's roles.
     *
     * @return the set of roles
     */
    Set<String> getRoles();

    /**
     * Returns the user's permissions.
     *
     * @return the set of permissions
     */
    Set<String> getPermissions();

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
