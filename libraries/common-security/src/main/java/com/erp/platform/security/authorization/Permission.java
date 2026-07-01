package com.erp.platform.security.authorization;

import java.util.Objects;

/**
 * Represents a permission in the system.
 *
 * <p>A permission is the finest-grained access control unit.
 * Permissions are typically grouped into roles, and roles
 * are assigned to users.
 *
 * <p>Permission format: {domain}:{action}:{resource}
 *
 * <p>Examples:
 * <ul>
 *   <li>invoice:read:invoice</li>
 *   <li>invoice:create:invoice</li>
 *   <li>invoice:update:invoice</li>
 *   <li>invoice:delete:invoice</li>
 *   <li>payment:process:payment</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class Permission {

    private final String domain;
    private final String action;
    private final String resource;

    private Permission(String domain, String action, String resource) {
        this.domain = domain;
        this.action = action;
        this.resource = resource;
    }

    /**
     * Creates a new permission.
     *
     * @param domain the domain (e.g., "invoice")
     * @param action the action (e.g., "read", "create", "update", "delete")
     * @param resource the resource (e.g., "invoice")
     * @return the permission
     */
    public static Permission of(String domain, String action, String resource) {
        return new Permission(domain, action, resource);
    }

    /**
     * Parses a permission string in the format {domain}:{action}:{resource}.
     *
     * @param permission the permission string
     * @return the permission
     * @throws IllegalArgumentException if the permission string is invalid
     */
    public static Permission parse(String permission) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Permission cannot be null or blank");
        }

        String[] parts = permission.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                "Invalid permission format. Expected: domain:action:resource, got: " + permission
            );
        }

        return new Permission(parts[0], parts[1], parts[2]);
    }

    /**
     * Returns the domain.
     *
     * @return the domain
     */
    public String domain() {
        return domain;
    }

    /**
     * Returns the action.
     *
     * @return the action
     */
    public String action() {
        return action;
    }

    /**
     * Returns the resource.
     *
     * @return the resource
     */
    public String resource() {
        return resource;
    }

    /**
     * Returns the permission as a string.
     *
     * @return the permission string
     */
    @Override
    public String toString() {
        return "%s:%s:%s".formatted(domain, action, resource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(domain, that.domain) &&
               Objects.equals(action, that.action) &&
               Objects.equals(resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain, action, resource);
    }
}
