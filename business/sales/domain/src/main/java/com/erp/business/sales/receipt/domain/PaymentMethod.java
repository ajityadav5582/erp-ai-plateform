package com.erp.business.sales.receipt.domain;

/**
 * How a customer settled a receivable.
 *
 * <p>A sales-local enum rather than a shared one, so the sales service carries no
 * dependency on the procurement module's payment vocabulary.
 */
public enum PaymentMethod {

    /** Cash on delivery or over the counter. */
    CASH,

    /** Bank transfer / NEFT / RTGS. */
    BANK_TRANSFER,

    /** Cheque. */
    CHEQUE,

    /** Debit or credit card. */
    CARD,

    /** Mobile / digital wallet (e.g. eSewa, Khalti, ConnectIPS). */
    DIGITAL_WALLET,

    /** Anything else, described in the receipt's reference. */
    OTHER
}
