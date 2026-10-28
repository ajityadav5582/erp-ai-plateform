package com.erp.business.sales.quotation.dto;

import com.erp.business.sales.quotation.domain.SalesQuotationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SalesQuotationResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String quotationNumber,
        LocalDate quotationDate,
        LocalDate validUntilDate,
        Long customerId,
        String customerCode,
        String customerName,
        String customerPanNumber,
        String currency,
        Long fiscalYearId,
        Long branchId,
        Long warehouseId,
        SalesQuotationStatus status,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Long salesOrderId,
        Instant convertedAt,
        String reference,
        String termsAndConditions,
        String notes,
        List<SalesQuotationLineResponse> lines
) {
}
