package com.erp.business.sales.salesreturn.domain;

/**
 * Lifecycle state of a sales return (credit note).
 *
 * <pre>
 *   DRAFT --post--> POSTED --cancel--> CANCELLED
 * </pre>
 *
 * <p>Posting is the act that restocks the returned goods and puts the credit note
 * on the sales register, where it nets against sales for the VAT return.
 */
public enum ReturnStatus {

    /** Editable. Lines can be added and removed freely. */
    DRAFT,

    /** Goods restocked and the credit note recorded. */
    POSTED,

    /** Voided after posting. */
    CANCELLED
}
