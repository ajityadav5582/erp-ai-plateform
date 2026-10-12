package com.erp.business.sales.invoice.dto;

import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model for a sales (tax) invoice, carrying the IRD mandatory fields and the
 * VAT register breakdown.
 */
public record SalesInvoiceResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String invoiceNumber,
        Long fiscalYearId,
        Long branchId,
        Long salesOrderId,
        String soNumber,
        Long deliveryId,
        String deliveryNumber,
        Long customerId,
        String customerCode,
        String customerName,
        String customerPanNumber,
        LocalDate invoiceDate,
        String invoiceDateNp,
        LocalDate dueDate,
        Boolean isTaxInvoice,
        String currency,
        SalesInvoiceStatus status,
        BigDecimal subtotalAmount,
        BigDecimal taxableAmount,
        BigDecimal exemptAmount,
        BigDecimal zeroRatedAmount,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceAmount,
        String notes,
        List<SalesInvoiceLineResponse> lines,
        Integer version
) {
}
