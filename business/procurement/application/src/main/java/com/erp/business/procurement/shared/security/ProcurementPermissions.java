package com.erp.business.procurement.shared.security;

/**
 * Permissions for the documents that close out the purchase process.
 *
 * <p>Goods receipt, supplier invoice and supplier payment are three different documents
 * with three different owners, which is why they are separate resources rather than
 * actions on one "procurement" permission. The person who takes delivery of the goods is
 * not the person who codes the invoice, and neither is the person who releases the
 * money.
 *
 * <p>Every code here must have a matching row in Identity's {@code permissions} table,
 * seeded by {@code DataInitializer.ensureSupportedPermissionsExist()}. A code with no row
 * can never be granted to a role, can never appear in an access token, and so closes the
 * annotated method to every caller including {@code SUPER_ADMIN}.
 *
 * @since 1.0.0
 */
public final class ProcurementPermissions {

    // ---- Goods receipt ----

    /** Read goods receipt notes and what they say arrived. */
    public static final String GOODS_RECEIPT_VIEW = "GOODS_RECEIPT_VIEW";

    /** Record a delivery against a purchase order. Held by storekeepers. */
    public static final String GOODS_RECEIPT_CREATE = "GOODS_RECEIPT_CREATE";

    /** Void a goods receipt note. */
    public static final String GOODS_RECEIPT_CANCEL = "GOODS_RECEIPT_CANCEL";

    // ---- Purchase invoice ----

    /** Read supplier invoices and their outstanding balances. */
    public static final String PURCHASE_INVOICE_VIEW = "PURCHASE_INVOICE_VIEW";

    /** Record a supplier invoice against a delivery. Held by accounts clerks. */
    public static final String PURCHASE_INVOICE_CREATE = "PURCHASE_INVOICE_CREATE";

    /** Void a supplier invoice. */
    public static final String PURCHASE_INVOICE_CANCEL = "PURCHASE_INVOICE_CANCEL";

    // ---- Supplier payment ----

    /** Read supplier payments. */
    public static final String SUPPLIER_PAYMENT_VIEW = "SUPPLIER_PAYMENT_VIEW";

    /** Pay a supplier against an invoice. Held by treasury. */
    public static final String SUPPLIER_PAYMENT_CREATE = "SUPPLIER_PAYMENT_CREATE";

    /** Reverse a payment. */
    public static final String SUPPLIER_PAYMENT_CANCEL = "SUPPLIER_PAYMENT_CANCEL";

    private ProcurementPermissions() {
        throw new AssertionError("No instances");
    }
}
