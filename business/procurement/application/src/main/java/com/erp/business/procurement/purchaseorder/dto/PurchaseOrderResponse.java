package com.erp.business.procurement.purchaseorder.dto;

import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderLineStatus;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A purchase order and its lines, as returned to the client. */
public record PurchaseOrderResponse(
        Long id,
        String poNumber,
        Long tenantId,
        Long companyId,
        Long branchId,
        Long fiscalYearId,

        /**
         * The requisition this order was converted from, or null for an order raised
         * directly. The client uses it to navigate back to the source document.
         */
        Long sourcePurchaseRequisitionId,

        PurchaseOrderStatus status,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        Long supplierId,
        String supplierCode,
        String supplierName,
        String currency,

        /**
         * The nominated destination, copied from the requisition at conversion.
         *
         * <p>Advisory rather than binding: the receipt may name a different
         * warehouse when goods are redirected, and comparing the two is how that
         * shows up. Null when neither the requisition nor the buyer settled one, in
         * which case receipt falls through to the company default.
         */
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String reference,
        String notes,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,

        /** Value of goods received so far. A money figure, not a quantity. */
        BigDecimal receivedAmount,

        /** Total minus received: the value still owed against this order. */
        BigDecimal outstandingAmount,
        Instant createdAt,
        List<Line> lines
) {

    public record Line(
            Long id,
            Integer lineNumber,
            Long itemId,
            String itemCode,
            String itemName,
            String uomCode,
            Long unitId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercentage,
            BigDecimal taxRate,
            BigDecimal lineAmount,
            BigDecimal receivedQuantity,
            PurchaseOrderLineStatus status,
            LocalDate expectedDeliveryDate,
            String notes,

            /**
             * The requisition line this came from, or null for a line raised directly.
             * Kept on the response so the details screen can show the trace without a
             * second request.
             */
            Long sourcePurchaseRequisitionItemId
    ) {
    }
}
