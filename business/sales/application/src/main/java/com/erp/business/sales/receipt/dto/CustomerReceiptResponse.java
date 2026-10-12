package com.erp.business.sales.receipt.dto;

import com.erp.business.sales.receipt.domain.PaymentMethod;
import com.erp.business.sales.receipt.domain.ReceiptStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read model for a customer receipt.
 */
public record CustomerReceiptResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String receiptNumber,
        Long fiscalYearId,
        Long branchId,
        Long invoiceId,
        String invoiceNumber,
        Long customerId,
        String customerCode,
        String customerName,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        LocalDate receiptDate,
        String reference,
        String notes,
        ReceiptStatus status,
        Integer version
) {
}
