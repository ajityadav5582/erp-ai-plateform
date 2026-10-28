package com.erp.business.sales.invoice.dto;

import com.erp.business.sales.invoice.domain.VatType;

import java.math.BigDecimal;

/**
 * One printed line of a tax invoice: what was sold and its VAT.
 */
public record TaxInvoiceLineView(
        Integer lineNumber,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercentage,
        VatType vatType,
        BigDecimal vatRate,
        BigDecimal lineAmount,
        BigDecimal vatAmount
) {
}
