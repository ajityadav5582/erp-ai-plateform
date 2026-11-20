package com.erp.business.procurement.purchasereturn;

import com.erp.business.procurement.purchasereturn.domain.PurchaseReturn;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnLine;
import com.erp.business.procurement.purchasereturn.dto.PurchaseReturnLineResponse;
import com.erp.business.procurement.purchasereturn.dto.PurchaseReturnResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PurchaseReturnMapper {

    public PurchaseReturnResponse toResponse(PurchaseReturn r) {
        if (r == null) return null;

        List<PurchaseReturnLineResponse> lineResponses = r.getLines() != null
                ? r.getLines().stream().map(this::toLineResponse).toList()
                : Collections.emptyList();

        return new PurchaseReturnResponse(
                r.getId(),
                r.getTenantId(),
                r.getCompanyId(),
                r.getDebitNoteNumber(),
                r.getFiscalYearId(),
                r.getBranchId(),
                r.getPurchaseInvoiceId(),
                r.getInvoiceNumber(),
                r.getGoodsReceiptId(),
                r.getGrnNumber(),
                r.getPurchaseOrderId(),
                r.getPoNumber(),
                r.getSupplierId(),
                r.getSupplierCode(),
                r.getSupplierName(),
                r.getSupplierPanNumber(),
                r.getWarehouseId(),
                r.getWarehouseCode(),
                r.getWarehouseName(),
                r.getReturnDate(),
                r.getReturnDateNp(),
                r.getReason(),
                r.getStatus(),
                r.getStockPostStatus(),
                r.getStockPostError(),
                r.getCurrency(),
                r.getSubtotalAmount(),
                r.getTaxAmount(),
                r.getTotalAmount(),
                r.getNotes(),
                r.getApprovedAt(),
                r.getApprovedBy(),
                r.getPostedAt(),
                r.getPostedBy(),
                r.getCancelledAt(),
                r.getCancelledBy(),
                r.getCancellationReason(),
                r.getCreatedAt(),
                lineResponses
        );
    }

    public PurchaseReturnLineResponse toLineResponse(PurchaseReturnLine line) {
        if (line == null) return null;
        return new PurchaseReturnLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getPurchaseInvoiceLineId(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getReturnQuantity(),
                line.getUnitPrice(),
                line.getTaxCategory(),
                line.getTaxRate(),
                line.getTaxableAmount(),
                line.getTaxAmount(),
                line.getSubtotalAmount(),
                line.getTotalAmount(),
                line.getRemarks()
        );
    }
}

