package com.erp.business.inventory.interfaces.stock;

import java.util.List;

/**
 * The outcome of posting (or recognising) a stock movement batch.
 *
 * @param referenceType  echoes the request's source document type
 * @param referenceId    echoes the request's source document id
 * @param postedCount    how many ledger rows this call created (0 on an idempotent replay)
 * @param alreadyExisted true when the batch was already posted and this call was a no-op
 * @param movementIds    the ids of the ledger rows for this reference, whether just
 *                       created or pre-existing
 */
public record StockMovementBatchResponse(
        String referenceType,
        Long referenceId,
        int postedCount,
        boolean alreadyExisted,
        List<Long> movementIds
) {
}
