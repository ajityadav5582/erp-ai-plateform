package com.erp.business.procurement.reports.vatreconciliation;

import java.math.BigDecimal;

public record ReconciliationSummary(
        int bsYear,
        int bsMonth,
        BigDecimal outputVatOnSales,
        BigDecimal inputVatOnPurchases,
        BigDecimal vatCreditFromCreditNotes,
        BigDecimal netVatPayable
) {
}
