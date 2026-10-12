package com.erp.business.procurement.supplierpayment.domain;

/**
 * Lifecycle of a supplier payment.
 *
 * <p>A payment is created POSTED. Money leaving the bank is not a request to do so, and
 * a pending state would only create the illusion that an un-posted payment can be
 * reviewed, when in practice a payment is approved before it is raised, not after.
 */
public enum SupplierPaymentStatus {

    /** Applied to the invoice. The normal state. */
    POSTED,

    /** Reversed. Terminal. */
    CANCELLED
}
