package com.erp.business.sales.shared.security;

/**
 * Permissions for the sales return (credit note) document.
 *
 * <p>Every code here must have a matching row in Identity's {@code permissions}
 * table, seeded by {@code DataInitializer}. A code with no row can never be granted
 * to a role, can never appear in an access token, and so closes the annotated
 * method to every caller including {@code SUPER_ADMIN}.
 *
 * @since 1.0.0
 */
public final class SalesReturnPermissions {

    /** Read sales returns / credit notes. */
    public static final String SALES_RETURN_VIEW = "SALES_RETURN_VIEW";

    /** Raise a draft credit note. */
    public static final String SALES_RETURN_CREATE = "SALES_RETURN_CREATE";

    /** Post a credit note, restocking the returned goods. */
    public static final String SALES_RETURN_POST = "SALES_RETURN_POST";

    /** Cancel a posted credit note. */
    public static final String SALES_RETURN_CANCEL = "SALES_RETURN_CANCEL";

    private SalesReturnPermissions() {
        throw new AssertionError("No instances");
    }
}
