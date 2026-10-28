package com.erp.business.procurement.purchaseinvoice.domain;

import java.math.BigDecimal;

/**
 * Lifecycle of a supplier purchase invoice.
 *
 * <p><pre>
 *   APPROVED -pay in full-> PAID
 *      |
 *      -pay in part-> PARTIALLY_PAID -pay in full-> PAID
 * </pre>
 *
 * <p>An invoice is created APPROVED, not SUBMITTED. A supplier invoice is a document
 * that already exists in the world: the supplier has issued it and the company has
 * received it. Adding a draft-approval step would mean asking someone to authorise a
 * document the supplier wrote. What is <em>not</em> assumed is that it matches the
 * order and the delivery - that mismatch is what the {@code amount} on the lines and
 * the reconciliation are for.
 */
public enum PurchaseInvoiceStatus {

    /** Recorded against the receipt, awaiting payment. The normal state. */
    APPROVED,

    /** Some, but not all, of the amount has been paid. */
    PARTIALLY_PAID,

    /** Fully settled. Terminal. */
    PAID,

    /** Voided. Terminal. */
    CANCELLED;

    /**
     * Derives the state from how much has been paid.
     *
     * <p>Derived rather than assigned so {@code paid_amount} and {@code status} cannot
     * drift apart, which is the same reasoning as
     * {@code PurchaseOrderLineStatus.fromReceivedQuantity}.
     *
     * @param totalAmount the invoice total, must be positive
     * @param paidAmount  the cumulative paid amount, must not be negative
     * @return {@link #PAID} when settled, {@link #PARTIALLY_PAID} when part-paid,
     *         otherwise {@link #APPROVED}
     */
    public static PurchaseInvoiceStatus fromPaidAmount(
            BigDecimal totalAmount, BigDecimal paidAmount) {

        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("Invoice total must be positive");
        }
        if (paidAmount == null || paidAmount.signum() < 0) {
            throw new IllegalArgumentException("Paid amount cannot be negative");
        }
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return PAID;
        }
        if (paidAmount.signum() > 0) {
            return PARTIALLY_PAID;
        }
        return APPROVED;
    }
}
