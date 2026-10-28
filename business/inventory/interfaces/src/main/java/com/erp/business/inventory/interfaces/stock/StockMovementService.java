package com.erp.business.inventory.interfaces.stock;

/**
 * Public contract for posting stock movements, consumed by other modules
 * (Procurement) without coupling to Inventory's internals.
 *
 * <p>Both operations are <strong>idempotent by reference</strong>. The
 * {@code (referenceType, referenceId)} pair names the source document, so a
 * caller that retries after a timeout or a crash is recognised: Inventory
 * returns the movements already posted for that reference instead of writing a
 * second set. A receipt posts once; its reversal posts once. The two are told
 * apart by direction, not by the reference, which is shared.
 *
 * <p>Tenant and company are resolved from the caller's authenticated context on
 * the Inventory side and are never taken from the request body.
 */
public interface StockMovementService {

    /**
     * Posts an inbound batch (goods arriving) for one source document.
     *
     * @param request the batch, carrying the reference and its lines
     * @return the outcome, including whether the batch already existed
     */
    StockMovementBatchResponse postReceipt(StockReceiptRequest request);

    /**
     * Posts an outbound batch (goods leaving) that reverses a prior receipt,
     * sharing the same reference so the ledger shows both the receipt and its
     * undo. The original rows are never deleted - the ledger is append-only.
     *
     * @param request the batch, carrying the reference and its lines
     * @return the outcome, including whether the reversal already existed
     */
    StockMovementBatchResponse reverseReceipt(StockReceiptRequest request);

    /**
     * Posts an outbound batch (goods leaving for a customer) for one source
     * document, for example a sales delivery. Shares the same idempotency-by-
     * reference contract as a receipt: a retry returns the movements already
     * posted rather than writing a second set.
     *
     * @param request the batch, carrying the reference and its lines
     * @return the outcome, including whether the batch already existed
     */
    StockMovementBatchResponse postIssue(StockReceiptRequest request);

    /**
     * Posts an outbound batch that reverses a prior issue (a voided delivery),
     * sharing the same reference so the ledger shows both the issue and its undo.
     * The original rows are never deleted - the ledger is append-only.
     *
     * @param request the batch, carrying the reference and its lines
     * @return the outcome, including whether the reversal already existed
     */
    StockMovementBatchResponse reverseIssue(StockReceiptRequest request);

    /**
     * Reserves stock against a demand (e.g. a confirmed sales order), moving
     * quantity into reserved so it is not available to other demand. Idempotent
     * by (referenceType, referenceId) keyed on RESERVE movements.
     */
    StockMovementBatchResponse reserve(StockReceiptRequest request);

    /**
     * Releases a prior reservation, returning quantity to available. Idempotent
     * by (referenceType, referenceId) keyed on RELEASE movements.
     */
    StockMovementBatchResponse release(StockReceiptRequest request);
}
