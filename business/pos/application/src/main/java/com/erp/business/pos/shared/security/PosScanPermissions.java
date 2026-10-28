package com.erp.business.pos.shared.security;

/**
 * Permissions for the POS terminal scan and pricing flow.
 *
 * <p>Every code must have a matching row in Identity's {@code permissions} table,
 * seeded by {@code DataInitializer}; a code with no row can never be granted and
 * closes the annotated method to every caller.
 */
public final class PosScanPermissions {

    /** Resolve a scanned code to a priced item, and preview cart totals. */
    public static final String POS_SCAN = "POS_SCAN";

    private PosScanPermissions() {
        throw new AssertionError("No instances");
    }
}
