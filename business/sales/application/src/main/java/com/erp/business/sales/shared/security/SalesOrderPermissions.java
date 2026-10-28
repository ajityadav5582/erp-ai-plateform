package com.erp.business.sales.shared.security;

/**
 * Permissions for the sales order document.
 *
 * <p>Every code here must have a matching row in Identity's {@code permissions}
 * table, seeded by {@code DataInitializer}. A code with no row can never be granted
 * to a role, can never appear in an access token, and so closes the annotated
 * method to every caller including {@code SUPER_ADMIN}.
 *
 * @since 1.0.0
 */
public final class SalesOrderPermissions {

    /** Read sales orders. */
    public static final String SALES_ORDER_VIEW = "SALES_ORDER_VIEW";

    /** Raise a draft sales order. */
    public static final String SALES_ORDER_CREATE = "SALES_ORDER_CREATE";

    /** Edit a draft sales order. */
    public static final String SALES_ORDER_EDIT = "SALES_ORDER_EDIT";

    /** Confirm a draft order, issuing it to the customer. */
    public static final String SALES_ORDER_CONFIRM = "SALES_ORDER_CONFIRM";

    /** Cancel an order. */
    public static final String SALES_ORDER_CANCEL = "SALES_ORDER_CANCEL";

    /** Close a delivered order. */
    public static final String SALES_ORDER_CLOSE = "SALES_ORDER_CLOSE";

    private SalesOrderPermissions() {
        throw new AssertionError("No instances");
    }
}
