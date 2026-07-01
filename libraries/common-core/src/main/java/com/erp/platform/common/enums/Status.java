package com.erp.platform.common.enums;

/**
 * Common status enum for entity lifecycle.
 *
 * <p>Provides a standard set of status values that can be used
 * across all business entities.
 *
 * @since 1.0.0
 */
public enum Status {
    /**
     * Entity is active and usable.
     */
    ACTIVE("Active"),

    /**
     * Entity is inactive but data is retained.
     */
    INACTIVE("Inactive"),

    /**
     * Entity is pending approval or activation.
     */
    PENDING("Pending"),

    /**
     * Entity is suspended temporarily.
     */
    SUSPENDED("Suspended"),

    /**
     * Entity is archived but data is retained.
     */
    ARCHIVED("Archived"),

    /**
     * Entity is deleted (soft delete).
     */
    DELETED("Deleted");

    private final String description;

    Status(String description) {
        this.description = description;
    }

    /**
     * Returns the description of the status.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if the status is active.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if the status is inactive.
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }

    /**
     * Checks if the status is pending.
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * Checks if the status is suspended.
     */
    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    /**
     * Checks if the status is archived.
     */
    public boolean isArchived() {
        return this == ARCHIVED;
    }

    /**
     * Checks if the status is deleted.
     */
    public boolean isDeleted() {
        return this == DELETED;
    }

    /**
     * Checks if the status is terminal (cannot be changed).
     */
    public boolean isTerminal() {
        return this == DELETED || this == ARCHIVED;
    }
}
