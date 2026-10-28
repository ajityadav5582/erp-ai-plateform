package com.erp.business.procurement.goodsreceipt;

import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;

import java.util.Map;

/**
 * Raised once a goods receipt has been committed, carrying the stock-movement
 * batch to post to Inventory.
 *
 * <p>Handled by a {@code @TransactionalEventListener(AFTER_COMMIT)} so the
 * network call to Inventory runs only after the receipt is durably saved, and
 * outside that transaction - the receipt survives Inventory being briefly down,
 * and a failure is recorded on the receipt for retry. The listener runs
 * {@code @Async}, so it executes off the request thread.
 *
 * <p>{@code authHeaders} are the caller's Inventory-bound headers (bearer token,
 * company, fiscal year) captured on the request thread when the event is
 * published. They travel with the event because the async worker has no
 * {@code RequestContextHolder} of its own; without them Inventory would refuse
 * the post. See {@code InventoryRequestContext}.
 *
 * @param tenantId    the owning tenant
 * @param companyId   the owning company
 * @param receiptId   the saved receipt's id
 * @param batch       the stock movements to post, built while the lines were managed
 * @param authHeaders the outbound auth/scope headers to forward to Inventory
 */
public record GoodsReceiptRecordedEvent(
        Long tenantId,
        Long companyId,
        Long receiptId,
        StockReceiptRequest batch,
        Map<String, String> authHeaders
) {
}
