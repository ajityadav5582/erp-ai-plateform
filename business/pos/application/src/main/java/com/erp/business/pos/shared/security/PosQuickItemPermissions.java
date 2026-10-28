package com.erp.business.pos.shared.security;

/**
 * Permissions for POS quick items (terminal grid tiles).
 *
 * <p>Every code must have a matching row in Identity's {@code permissions} table,
 * seeded by {@code DataInitializer}; a code with no row can never be granted and
 * closes the annotated method to every caller.
 */
public final class PosQuickItemPermissions {

    /** Read POS quick items. */
    public static final String POS_QUICK_ITEM_VIEW = "POS_QUICK_ITEM_VIEW";

    /** Add a tile to the terminal grid. */
    public static final String POS_QUICK_ITEM_CREATE = "POS_QUICK_ITEM_CREATE";

    /** Edit a tile's item, price override, colour, icon or position. */
    public static final String POS_QUICK_ITEM_UPDATE = "POS_QUICK_ITEM_UPDATE";

    /** Remove a tile from the grid. */
    public static final String POS_QUICK_ITEM_DELETE = "POS_QUICK_ITEM_DELETE";

    private PosQuickItemPermissions() {
        throw new AssertionError("No instances");
    }
}
