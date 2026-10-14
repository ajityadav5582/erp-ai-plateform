package com.erp.business.sales.quotation.domain;

/**
 * Lifecycle state of a commercial sales quotation.
 */
public enum SalesQuotationStatus {
    /** Editable draft. */
    DRAFT,

    /** Sent or issued to the customer. */
    SENT,

    /** Accepted by the customer, ready to be converted into an active Sales Order. */
    ACCEPTED,

    /** Rejected or declined by the customer. */
    REJECTED,

    /** Validity period expired without conversion. */
    EXPIRED,

    /** Successfully converted into a formal Sales Order. */
    CONVERTED
}
