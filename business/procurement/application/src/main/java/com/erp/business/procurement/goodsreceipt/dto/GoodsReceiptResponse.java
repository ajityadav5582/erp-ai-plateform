package com.erp.business.procurement.goodsreceipt.dto;

import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptStatus;
import com.erp.business.procurement.goodsreceipt.domain.StockPostStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A goods receipt note and its lines, as returned to the client. */
public record GoodsReceiptResponse(
        Long id,
        String grnNumber,
        Long tenantId,
        Long companyId,
        Long branchId,
        Long fiscalYearId,

        /** The order this receipt was raised against. */
        Long purchaseOrderId,
        String poNumber,

        LocalDate receiptDate,
        Long supplierId,
        String supplierCode,
        String supplierName,
        String currency,
        GoodsReceiptStatus status,

        /**
         * Whether the stock movement has been landed in Inventory, and why not
         * when it has not. A receipt is recorded on arrival regardless; this
         * tells the operator whether the ledger caught up.
         */
        StockPostStatus stockPostStatus,
        String stockPostError,

        /** Net value of the goods received on this note. */
        BigDecimal receivedAmount,

        /**
         * Where the goods physically landed. Always resolved by the time the note is
         * posted - an explicit choice, the order's nomination, or the company default.
         */
        Long warehouseId,
        String warehouseCode,
        String warehouseName,

        String deliveryChallanNo,
        String notes,
        Instant postedAt,
        String postedBy,
        String cancellationReason,
        Instant createdAt,
        List<Line> lines
) {

    public record Line(
            Long id,
            Integer lineNumber,

            /** The order line this receipt satisfies. */
            Long purchaseOrderLineId,

            Long itemId,
            String itemCode,
            String itemName,
            String uomCode,
            Long unitId,
            BigDecimal receivedQuantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount,

            /**
             * The warehouse this line's quantity landed in. Normally the note's
             * warehouse, and different from it only when one consignment was split
             * across two sites.
             */
            Long warehouseId,

            String remarks
    ) {
    }
}
