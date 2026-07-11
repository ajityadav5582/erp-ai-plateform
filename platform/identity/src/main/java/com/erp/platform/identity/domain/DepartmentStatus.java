package com.erp.platform.identity.domain;

/**
 * Department status enumeration.
 *
 * <p>Represents the lifecycle states of a department in the system.
 *
 * @since 1.0.0
 */
public enum DepartmentStatus {

    /**
     * Department is active and operational.
     * Users and teams can be assigned to active departments.
     */
    ACTIVE,

    /**
     * Department is inactive and not operational.
     * No new assignments should be made to inactive departments.
     * Existing assignments are preserved for audit purposes.
     */
    INACTIVE
}
