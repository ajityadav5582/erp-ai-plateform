package com.erp.business.sales.invoice.dto;

import com.erp.business.sales.invoice.domain.VatType;

import java.math.BigDecimal;

/**
 * Read model for one sales-invoice line, including its VAT treatment.
 */
public record SalesInvoiceLineResponse(
        Long id,
        Integer lineNumber,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercentage,
        VatType vatType,
        BigDecimal vatRate,
        BigDecimal lineAmount,
        BigDecimal vatAmount,
        String description
) {
}
