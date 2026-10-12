package com.erp.business.sales.shared.security;

/**
 * Permissions for the customer receipt document.
 *
 * <p>Every code here must have a matching row in Identity's {@code permissions}
 * table, seeded by {@code DataInitializer}. A code with no row can never be granted
 * to a role, can never appear in an access token, and so closes the annotated
 * method to every caller including {@code SUPER_ADMIN}.
 *
 * @since 1.0.0
 */
public final class CustomerReceiptPermissions {

    /** Read customer receipts. */
    public static final String CUSTOMER_RECEIPT_VIEW = "CUSTOMER_RECEIPT_VIEW";

    /** Record a receipt against an invoice. Held by treasury / cashier. */
    public static final String CUSTOMER_RECEIPT_CREATE = "CUSTOMER_RECEIPT_CREATE";

    /** Reverse a posted receipt. */
    public static final String CUSTOMER_RECEIPT_CANCEL = "CUSTOMER_RECEIPT_CANCEL";

    private CustomerReceiptPermissions() {
        throw new AssertionError("No instances");
    }
}
