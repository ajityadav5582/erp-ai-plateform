package com.erp.platform.identity.domain;

/**
 * Branch status enumeration.
 *
 * <p>Represents the lifecycle states of a branch in the system.
 *
 * @since 1.0.0
 */
public enum BranchStatus {

    /**
     * Branch is active and operational.
     * Users, departments, and assets can be assigned to active branches.
     */
    ACTIVE,

    /**
     * Branch is inactive and not operational.
     * No new assignments should be made to inactive branches.
     * Existing assignments are preserved for audit purposes.
     */
    INACTIVE
}
