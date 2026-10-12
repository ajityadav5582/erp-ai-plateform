package com.erp.business.sales.shared.security;

/**
 * Permissions for the sales delivery (goods issue) document.
 *
 * <p>Every code here must have a matching row in Identity's {@code permissions}
 * table, seeded by {@code DataInitializer}. A code with no row can never be granted
 * to a role, can never appear in an access token, and so closes the annotated
 * method to every caller including {@code SUPER_ADMIN}.
 *
 * @since 1.0.0
 */
public final class SalesDeliveryPermissions {

    /** Read sales deliveries. */
    public static final String SALES_DELIVERY_VIEW = "SALES_DELIVERY_VIEW";

    /** Raise a draft delivery against a sales order. */
    public static final String SALES_DELIVERY_CREATE = "SALES_DELIVERY_CREATE";

    /** Post a delivery, issuing the goods from stock. Held by storekeepers. */
    public static final String SALES_DELIVERY_POST = "SALES_DELIVERY_POST";

    /** Cancel a posted delivery, restoring stock. */
    public static final String SALES_DELIVERY_CANCEL = "SALES_DELIVERY_CANCEL";

    private SalesDeliveryPermissions() {
        throw new AssertionError("No instances");
    }
}
