package com.erp.business.procurement.supplierpayment.dto;

import java.math.BigDecimal;

/**
 * Real-time balance summary for a supplier: total outstanding payables across unpaid
 * and partially-paid invoices, and total unallocated advance payments available.
 */
public record SupplierBalanceSummaryResponse(
        Long supplierId,
        BigDecimal totalOutstandingPayable,
        BigDecimal totalAvailableAdvance,
        int unpaidInvoiceCount,
        int availableAdvanceCount,
        String currency
) {
}

