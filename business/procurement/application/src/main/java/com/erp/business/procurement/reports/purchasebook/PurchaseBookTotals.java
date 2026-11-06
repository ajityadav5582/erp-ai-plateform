package com.erp.business.procurement.reports.purchasebook;

import java.math.BigDecimal;

public record PurchaseBookTotals(
        BigDecimal totalPurchaseAmount,
        BigDecimal exemptPurchaseAmount,
        BigDecimal taxablePurchaseAmount,
        BigDecimal inputVatAmount,
        BigDecimal importTaxableAmount,
        BigDecimal importVatAmount
) {
}
