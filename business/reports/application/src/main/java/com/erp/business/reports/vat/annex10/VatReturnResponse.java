package com.erp.business.reports.vat.annex10;

import java.math.BigDecimal;

public record VatReturnResponse(
        Long companyId,
        Long fiscalYearId,
        int nepaliMonth,
        // Section 1: Sales / Output VAT
        BigDecimal totalSales,
        BigDecimal taxExemptSales,
        BigDecimal zeroRatedExports,
        BigDecimal taxableSales,
        BigDecimal outputVatCollected,
        BigDecimal lessSalesReturnVat,
        BigDecimal netOutputVatPayable,
        // Section 2: Purchase / Input VAT
        BigDecimal totalLocalPurchases,
        BigDecimal taxExemptPurchases,
        BigDecimal taxableLocalPurchases,
        BigDecimal importPurchasesTaxable,
        BigDecimal capitalGoodsPurchases,
        BigDecimal totalEligibleInputVat,
        BigDecimal lessPurchaseReturnVat,
        BigDecimal netEligibleInputVat,
        // Section 3: Settlement
        BigDecimal previousMonthCreditBroughtForward,
        BigDecimal netVatPayableToGoN,
        BigDecimal netVatCarriedForward
) {}
