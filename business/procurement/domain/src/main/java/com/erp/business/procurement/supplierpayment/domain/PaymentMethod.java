package com.erp.business.procurement.supplierpayment.domain;

/**
 * How money left the company to reach the supplier.
 *
 * <p>Recorded on the payment rather than derived, because the method is what the bank
 * reconciliation later matches against: a cheque number and a bank transfer reference
 * are the two things an auditor asks for.
 */
public enum PaymentMethod {

    /** Cash handed over. */
    CASH,

    /** Bank transfer, with the transaction reference on the payment. */
    BANK_TRANSFER,

    /** Cheque, with the cheque number on the payment. */
    CHEQUE,

    /** Card. */
    CARD,

    /** Any other instrument the company records by its own description. */
    OTHER
}
