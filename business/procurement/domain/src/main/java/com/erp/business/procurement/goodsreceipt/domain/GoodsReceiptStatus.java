package com.erp.business.procurement.goodsreceipt.domain;

/**
 * Lifecycle of a goods receipt note.
 *
 * <p>A receipt is created already posted, because receiving is the point at which the
 * goods physically arrive and the purchase order becomes inventory. A DRAFT state was
 * deliberately left out: there is nothing to decide between "keyed in" and "keyed in
 * again", and a receipt that existed without having moved the order would let the
 * receipt worklist disagree with the order totals.
 */
public enum GoodsReceiptStatus {

    /** Receipt applied to the purchase order. The normal state. */
    POSTED,

    /** Voided. Terminal. */
    CANCELLED
}
