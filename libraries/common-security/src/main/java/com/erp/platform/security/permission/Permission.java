package com.erp.platform.security.permission;

import java.io.Serializable;

/**
 * Permission abstraction.
 *
 * <p>Represents a fine-grained permission that can be granted to users.
 * Permissions are typically used for authorization decisions.
 *
 * @since 1.0.0
 */
public record Permission(
    String name,
    String description,
    String resource,
    String action
) implements Serializable {

    /**
     * Creates a permission from a string in format "resource:action".
     *
     * @param permission the permission string
     * @return a new Permission
     */
    public static Permission of(String permission) {
        String[] parts = permission.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid permission format: " + permission);
        }
        return new Permission(permission, "", parts[0], parts[1]);
    }

    /**
     * Creates a permission with all fields.
     */
    public static Permission of(String name, String description, String resource, String action) {
        return new Permission(name, description, resource, action);
    }

    /**
     * Returns the permission as a string.
     *
     * @return the permission string
     */
    public String asString() {
        return resource + ":" + action;
    }

    @Override
    public String toString() {
        return asString();
    }
}
