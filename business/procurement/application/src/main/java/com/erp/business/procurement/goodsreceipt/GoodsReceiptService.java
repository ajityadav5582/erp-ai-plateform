package com.erp.business.procurement.goodsreceipt;

import com.erp.business.procurement.goodsreceipt.dto.CreateGoodsReceiptRequest;
import com.erp.business.procurement.goodsreceipt.dto.GoodsReceiptResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Records goods arrivals against issued purchase orders.
 *
 * <p>This is the step that turns a promise into stock. It is also the only place
 * {@code receivedQuantity} on an order line is ever advanced, which is why the order's
 * totals and its receipt notes cannot drift apart.
 */
public interface GoodsReceiptService {

    /**
     * Records a delivery and applies it to the order.
     *
     * <p>Both happen in one transaction: a receipt note that exists without the order
     * having moved, or an order that moved without a note, would each be a state nobody
     * can reconcile.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param request   the order, the lines that arrived, and the delivery details
     * @param actor     the acting user id
     * @return the created goods receipt note
     */
    GoodsReceiptResponse record(Long tenantId, Long companyId,
                                CreateGoodsReceiptRequest request, String actor);

    /**
     * Voids a receipt and gives its quantities back to the order.
     *
     * <p>Both halves happen in one transaction, and that pairing is the whole
     * point. Voiding the note alone would leave the order holding quantities that
     * never arrived, and the deliveries could then never be booked against it
     * again - the goods would be physically present and invisible to the system.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the receipt to void
     * @param reason    why it is being voided, required
     * @param actor     the acting user id
     * @return the voided receipt
     */
    GoodsReceiptResponse cancel(Long tenantId, Long companyId, Long id,
                                 String reason, String actor);

    /**
     * Re-attempts landing a receipt's stock movement in Inventory.
     *
     * <p>For a receipt whose stock post failed (Inventory was unreachable when
     * the receipt was recorded). The call is idempotent on the Inventory side,
     * so retrying a receipt that in fact posted is a safe no-op, and retrying
     * one that did not completes it. A receipt already POSTED is returned
     * unchanged.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the receipt to retry
     * @return the receipt with its updated stock-post status
     */
    GoodsReceiptResponse retryStockPost(Long tenantId, Long companyId, Long id);

    GoodsReceiptResponse getById(Long tenantId, Long companyId, Long id);

    /**
     * The receipts raised against an order.
     */
    Page<GoodsReceiptResponse> findByOrder(Long tenantId, Long companyId, Long orderId,
                                          Pageable pageable);

    /**
     * Every receipt in the caller's scope.
     */
    Page<GoodsReceiptResponse> findAll(Long tenantId, Long companyId, Pageable pageable);
}
