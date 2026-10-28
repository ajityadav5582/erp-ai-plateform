package com.erp.business.procurement.purchaseorder;

import com.erp.business.procurement.purchaseorder.domain.PurchaseOrder;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a purchase order aggregate onto its client representation.
 *
 * <p>Extracted so that the conversion service and the lifecycle service cannot drift
 * apart on what the client is told. Two copies of this mapping would eventually disagree
 * about a field, and the disagreement would only surface as an unexplained difference
 * between the response to a create call and the response to a later get.
 *
 * <p>A component rather than static methods so it can grow with the document without the
 * call sites changing.
 */
@Component
public class PurchaseOrderAssembler {

    /**
     * Builds the full order response, including its lines.
     *
     * <p>{@code lineAmount} is the database-generated column, which is null on a line
     * that has not been flushed. The Java-side {@code netAmount()} covers that case so a
     * freshly created order reports the same totals the caller can immediately see on a
     * re-read.
     *
     * @param order the order to map
     * @return the client representation
     */
    public PurchaseOrderResponse toResponse(PurchaseOrder order) {
        List<PurchaseOrderResponse.Line> lines = new ArrayList<>();
        for (PurchaseOrderLine line : order.getLines()) {
            lines.add(new PurchaseOrderResponse.Line(
                    line.getId(),
                    line.getLineNumber(),
                    line.getItemId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getUomCode(),
                    line.getUnitId(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getDiscountPercentage(),
                    line.getTaxRate(),
                    line.getLineAmount() != null ? line.getLineAmount() : line.netAmount(),
                    line.getReceivedQuantity(),
                    line.getStatus(),
                    line.getExpectedDeliveryDate(),
                    line.getNotes(),
                    line.getSourcePurchaseRequisitionItemId()));
        }

        return new PurchaseOrderResponse(
                order.getId(),
                order.getPoNumber(),
                order.getTenantId(),
                order.getCompanyId(),
                order.getBranchId(),
                order.getFiscalYearId(),
                order.getSourcePurchaseRequisitionId(),
                order.getStatus(),
                order.getOrderDate(),
                order.getExpectedDeliveryDate(),
                order.getSupplierId(),
                order.getSupplierCode(),
                order.getSupplierName(),
                order.getCurrency(),
                order.getWarehouseId(),
                order.getWarehouseCode(),
                order.getWarehouseName(),
                order.getReference(),
                order.getNotes(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getTaxAmount(),
                order.getShippingAmount(),
                order.getTotalAmount(),
                order.getReceivedAmount(),
                order.outstandingAmount(),
                order.getCreatedAt(),
                lines);
    }
}
