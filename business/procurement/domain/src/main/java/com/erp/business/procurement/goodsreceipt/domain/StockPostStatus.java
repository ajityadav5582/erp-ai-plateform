package com.erp.business.procurement.goodsreceipt.domain;

/**
 * Whether a posted goods receipt's stock movements have been landed in the
 * Inventory ledger.
 *
 * <p>A receipt is recorded the moment the goods physically arrive, independently
 * of whether Inventory is reachable to post the movement. This status tracks
 * that second step so a failure is visible and retriable rather than silently
 * lost: the receipt is real business data either way, and refusing to record it
 * because Inventory was briefly down would lose the delivery.
 */
public enum StockPostStatus {

    /** Recorded; the stock movement has not been confirmed yet. */
    PENDING,

    /** Inventory accepted the stock movement. */
    POSTED,

    /** Posting to Inventory failed; the receipt is kept and can be retried. */
    FAILED
}
