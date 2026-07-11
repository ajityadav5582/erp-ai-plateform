package com.erp.platform.identity.domain;

/**
 * Permission status enumeration.
 *
 * <p>Represents the lifecycle states of a permission in the system.
 *
 * @since 1.0.0
 */
public enum PermissionStatus {

    /**
     * Permission is active and can be assigned to roles.
     */
    ACTIVE,

    /**
     * Permission is inactive and cannot be assigned to roles.
     * Inactive permissions are preserved for audit and historical purposes.
     */
    INACTIVE
}
