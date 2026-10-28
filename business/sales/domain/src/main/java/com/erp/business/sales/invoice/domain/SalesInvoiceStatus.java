package com.erp.business.sales.invoice.domain;

/**
 * Lifecycle state of a sales (tax) invoice.
 *
 * <pre>
 *   DRAFT --issue--> ISSUED --partial payment--> PARTIALLY_PAID --full payment--> PAID
 *     |                 |
 *     +---cancel------>+---cancel--> CANCELLED
 * </pre>
 *
 * <p>Payment progress is derived, never hand-picked: {@link #fromPaidAmount}
 * moves an issued invoice between ISSUED, PARTIALLY_PAID and PAID as receipts are
 * applied, so the status can never disagree with {@code paidAmount}.
 */
public enum SalesInvoiceStatus {

    /** Editable. Not yet issued to the customer. */
    DRAFT,

    /** Issued to the customer. Awaiting settlement. */
    ISSUED,

    /** Some payment received, balance outstanding. */
    PARTIALLY_PAID,

    /** Fully settled. */
    PAID,

    /** Voided. Terminal; a credit note is the correct instrument for a posted return. */
    CANCELLED;

    /**
     * Derives the payment state of an issued invoice from its total and paid
     * amounts. A cancelled invoice is left untouched by the caller.
     */
    public static SalesInvoiceStatus fromPaidAmount(java.math.BigDecimal total,
                                                    java.math.BigDecimal paid) {
        if (paid.signum() <= 0) {
            return ISSUED;
        }
        return paid.compareTo(total) >= 0 ? PAID : PARTIALLY_PAID;
    }
}
