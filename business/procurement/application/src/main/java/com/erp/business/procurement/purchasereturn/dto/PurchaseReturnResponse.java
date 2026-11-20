package com.erp.business.procurement.purchasereturn.dto;

import com.erp.business.procurement.goodsreceipt.domain.StockPostStatus;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseReturnResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String debitNoteNumber,
        Long fiscalYearId,
        Long branchId,
        Long purchaseInvoiceId,
        String invoiceNumber,
        Long goodsReceiptId,
        String grnNumber,
        Long purchaseOrderId,
        String poNumber,
        Long supplierId,
        String supplierCode,
        String supplierName,
        String supplierPanNumber,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        LocalDate returnDate,
        String returnDateNp,
        String reason,
        PurchaseReturnStatus status,
        StockPostStatus stockPostStatus,
        String stockPostError,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String notes,
        Instant approvedAt,
        String approvedBy,
        Instant postedAt,
        String postedBy,
        Instant cancelledAt,
        String cancelledBy,
        String cancellationReason,
        Instant createdAt,
        List<PurchaseReturnLineResponse> lines
) {}

