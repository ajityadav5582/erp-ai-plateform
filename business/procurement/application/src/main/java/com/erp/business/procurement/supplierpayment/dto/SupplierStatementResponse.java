package com.erp.business.procurement.supplierpayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Complete chronological Statement of Account for a supplier across a given date range.
 */
public record SupplierStatementResponse(
        Long supplierId,
        String supplierCode,
        String supplierName,
        String currency,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal openingBalance,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalReturns,
        BigDecimal closingBalance,
        List<SupplierStatementLineResponse> lines
) {
}

