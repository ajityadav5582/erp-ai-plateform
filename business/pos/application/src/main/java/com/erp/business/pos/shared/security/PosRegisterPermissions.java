package com.erp.business.pos.shared.security;

/**
 * Permissions for POS register (terminal) management.
 *
 * <p>Every code must have a matching row in Identity's {@code permissions} table,
 * seeded by {@code DataInitializer}; a code with no row can never be granted and
 * closes the annotated method to every caller.
 */
public final class PosRegisterPermissions {

    /** Read POS registers. */
    public static final String POS_REGISTER_VIEW = "POS_REGISTER_VIEW";

    /** Create a POS register. */
    public static final String POS_REGISTER_CREATE = "POS_REGISTER_CREATE";

    /** Edit a POS register's details. */
    public static final String POS_REGISTER_UPDATE = "POS_REGISTER_UPDATE";

    /** Deactivate or reactivate a POS register. */
    public static final String POS_REGISTER_MANAGE = "POS_REGISTER_MANAGE";

    private PosRegisterPermissions() {
        throw new AssertionError("No instances");
    }
}
