package com.erp.business.procurement.goodsreceipt;

import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;

/**
 * Raised once a goods receipt has been committed, carrying the stock-movement
 * batch to post to Inventory.
 *
 * <p>Handled by a {@code @TransactionalEventListener(AFTER_COMMIT)} so the
 * network call to Inventory runs only after the receipt is durably saved, and
 * outside that transaction - the receipt survives Inventory being briefly down,
 * and a failure is recorded on the receipt for retry.
 *
 * @param tenantId  the owning tenant
 * @param companyId the owning company
 * @param receiptId the saved receipt's id
 * @param batch     the stock movements to post, built while the lines were managed
 */
public record GoodsReceiptRecordedEvent(
        Long tenantId,
        Long companyId,
        Long receiptId,
        StockReceiptRequest batch
) {
}
