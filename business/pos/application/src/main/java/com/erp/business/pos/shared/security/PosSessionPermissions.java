package com.erp.business.pos.shared.security;

/**
 * Permissions for POS cashier sessions (shift open/close).
 *
 * <p>Every code must have a matching row in Identity's {@code permissions} table,
 * seeded by {@code DataInitializer}; a code with no row can never be granted and
 * closes the annotated method to every caller.
 */
public final class PosSessionPermissions {

    /** Read POS sessions. */
    public static final String POS_SESSION_VIEW = "POS_SESSION_VIEW";

    /** Open a cashier session on a register. */
    public static final String POS_SESSION_OPEN = "POS_SESSION_OPEN";

    /** Close a cashier session and record the cash count. */
    public static final String POS_SESSION_CLOSE = "POS_SESSION_CLOSE";

    private PosSessionPermissions() {
        throw new AssertionError("No instances");
    }
}
