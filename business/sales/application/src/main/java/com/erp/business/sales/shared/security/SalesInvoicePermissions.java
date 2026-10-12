package com.erp.business.sales.shared.security;

/**
 * Permissions for the sales (tax) invoice document.
 *
 * <p>Every code here must have a matching row in Identity's {@code permissions}
 * table, seeded by {@code DataInitializer}. A code with no row can never be granted
 * to a role, can never appear in an access token, and so closes the annotated
 * method to every caller including {@code SUPER_ADMIN}.
 *
 * @since 1.0.0
 */
public final class SalesInvoicePermissions {

    /** Read sales invoices and their outstanding balances. */
    public static final String SALES_INVOICE_VIEW = "SALES_INVOICE_VIEW";

    /** Raise a draft sales invoice. */
    public static final String SALES_INVOICE_CREATE = "SALES_INVOICE_CREATE";

    /** Issue a draft invoice to the customer. The IRD-relevant act. */
    public static final String SALES_INVOICE_ISSUE = "SALES_INVOICE_ISSUE";

    /** Void an issued invoice. */
    public static final String SALES_INVOICE_CANCEL = "SALES_INVOICE_CANCEL";

    private SalesInvoicePermissions() {
        throw new AssertionError("No instances");
    }
}
