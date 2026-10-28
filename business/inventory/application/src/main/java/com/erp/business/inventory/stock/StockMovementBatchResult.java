package com.erp.business.inventory.stock;

import com.erp.business.inventory.domain.StockMovement;

import java.util.List;

/**
 * Outcome of an idempotent batch post: the ledger rows for the reference, and
 * whether they were already there (a replay) rather than created by this call.
 *
 * @param movements      every ledger row for the reference in this direction
 * @param alreadyExisted true when the batch was recognised as already applied
 */
public record StockMovementBatchResult(List<StockMovement> movements, boolean alreadyExisted) {

    /** How many rows this call actually created (zero on a replay). */
    public int postedCount() {
        return alreadyExisted ? 0 : movements.size();
    }
}
