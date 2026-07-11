package com.erp.platform.identity.domain;

/**
 * User status enumeration.
 *
 * <p>Represents the lifecycle states of a user account in the system.
 *
 * @since 1.0.0
 */
public enum UserStatus {

    /**
     * User account has been created but not yet activated.
     * The user cannot log in until activated.
     */
    PENDING_ACTIVATION,

    /**
     * User account is active and can log in.
     * This is the normal operating state for active users.
     */
    ACTIVE,

    /**
     * User account is locked and cannot log in.
     * Typically set after multiple failed login attempts or by an administrator.
     */
    LOCKED,

    /**
     * User account has been deactivated.
     * The user cannot log in, but the account data is preserved.
     */
    DEACTIVATED,

    /**
     * User account has been archived.
     * This is a terminal state - archived users cannot be reactivated.
     * Used for compliance and long-term data retention.
     */
    ARCHIVED
}
