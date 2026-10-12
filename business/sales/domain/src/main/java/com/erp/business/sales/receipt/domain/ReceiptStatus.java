package com.erp.business.sales.receipt.domain;

/**
 * Lifecycle state of a customer receipt.
 *
 * <pre>
 *   (created) POSTED --cancel--> CANCELLED
 * </pre>
 *
 * <p>A receipt is recorded POSTED: receiving money is the act, there is no draft.
 * Cancelling reverses the payment against the invoice.
 */
public enum ReceiptStatus {

    /** Money received and applied to the invoice. */
    POSTED,

    /** Reversed. The invoice's paid amount is rolled back. */
    CANCELLED
}
