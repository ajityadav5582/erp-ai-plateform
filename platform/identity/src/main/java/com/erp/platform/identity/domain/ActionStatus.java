package com.erp.platform.identity.domain;

/**
 * Action status enumeration.
 *
 * <p>Represents the lifecycle states of an action in the system.
 *
 * @since 1.0.0
 */
public enum ActionStatus {

    /**
     * Action is active and can be used to create permissions.
     */
    ACTIVE,

    /**
     * Action is inactive and cannot be used to create permissions.
     * Inactive actions are preserved for audit and historical purposes.
     */
    INACTIVE
}
