package com.erp.business.procurement.reports.purchasebook;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of the statutory VAT Purchase Book (खरिद खाता - Annex 7).
 */
public record PurchaseBookRow(
        String mitiBs,
        LocalDate dateAd,
        String invoiceNumber,
        String customDeclarationNumber,
        String supplierName,
        String supplierPan,
        BigDecimal totalPurchaseAmount,
        BigDecimal exemptPurchaseAmount,
        BigDecimal taxablePurchaseAmount,
        BigDecimal inputVatAmount,
        BigDecimal importTaxableAmount,
        BigDecimal importVatAmount,
        boolean capitalAsset
) {
}
