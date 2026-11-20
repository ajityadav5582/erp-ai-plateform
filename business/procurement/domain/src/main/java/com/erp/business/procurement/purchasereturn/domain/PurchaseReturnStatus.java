package com.erp.business.procurement.purchasereturn.domain;

/**
 * Lifecycle states of a Purchase Return / Debit Note document.
 */
public enum PurchaseReturnStatus {

    /** Created and editable; not yet committed. */
    DRAFT,

    /** Reviewed and approved for return shipment / debit note issuance. */
    APPROVED,

    /** Finalized; stock deducted from warehouse and accounts payable balance adjusted. */
    POSTED,

    /** Revoked; reversing stock and AP balance adjustments. */
    CANCELLED
}

