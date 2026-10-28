package com.erp.business.sales.salesreturn.dto;

import com.erp.business.sales.invoice.domain.VatType;

import java.math.BigDecimal;

/**
 * Read model for one sales-return (credit note) line.
 */
public record SalesReturnLineResponse(
        Long id,
        Integer lineNumber,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        VatType vatType,
        BigDecimal vatRate,
        BigDecimal lineAmount,
        BigDecimal vatAmount,
        String description
) {
}
