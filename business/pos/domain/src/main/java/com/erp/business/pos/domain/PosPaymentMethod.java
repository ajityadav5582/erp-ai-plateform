package com.erp.business.pos.domain;

/**
 * How the customer settled a POS transaction.
 *
 * <p>A POS-local enum rather than a shared platform enum, keeping the POS
 * module free of coupling to other modules' payment vocabulary.
 *
 * <p>Digital wallets (eSewa, Khalti, ConnectIPS) are grouped under
 * {@link #DIGITAL_WALLET}; the specific provider can be captured in the
 * transaction's {@code paymentReference} field if needed for reconciliation.
 */
public enum PosPaymentMethod {

    /** Physical cash handed over at the counter. */
    CASH,

    /** Debit or credit card (POS terminal swipe / tap). */
    CARD,

    /** Mobile digital wallet — eSewa, Khalti, ConnectIPS, etc. */
    DIGITAL_WALLET,

    /** Direct bank transfer / NEFT / RTGS — rare for POS but supported. */
    BANK_TRANSFER,

    /** Cheque — uncommon for retail POS but included for completeness. */
    CHEQUE,

    /** Any other method; the reference field should describe it. */
    OTHER
}
