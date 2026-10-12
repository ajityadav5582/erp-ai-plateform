package com.erp.business.procurement.purchaseinvoice.dto;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A supplier invoice and its lines, as returned to the client. */
public record PurchaseInvoiceResponse(
        Long id,
        String invoiceNumber,
        Long tenantId,
        Long companyId,
        Long branchId,
        Long fiscalYearId,

        /** The order whose goods were billed. */
        Long purchaseOrderId,
        String poNumber,

        /** The delivery being billed. One invoice per delivery. */
        Long goodsReceiptId,
        String grnNumber,

        LocalDate invoiceDate,
        LocalDate dueDate,
        Long supplierId,
        String supplierCode,
        String supplierName,
        String currency,
        PurchaseInvoiceStatus status,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,

        /** How much has been settled so far. */
        BigDecimal paidAmount,

        /** What is still owed: total minus paid. The accounts payable figure. */
        BigDecimal balanceAmount,

        String notes,
        String cancellationReason,
        Instant createdAt,
        List<Line> lines
) {

    public record Line(
            Long id,
            Integer lineNumber,
            Long goodsReceiptLineId,
            Long itemId,
            String itemCode,
            String itemName,
            String uomCode,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercentage,
            BigDecimal taxRate,
            BigDecimal lineAmount,
            BigDecimal lineTaxAmount,
            String description
    ) {
    }
}
