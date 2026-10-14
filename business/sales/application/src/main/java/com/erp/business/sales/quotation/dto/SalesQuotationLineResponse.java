package com.erp.business.sales.quotation.dto;

import java.math.BigDecimal;

public record SalesQuotationLineResponse(
        Long id,
        Integer lineNumber,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercentage,
        BigDecimal taxRate,
        BigDecimal lineAmount,
        String description
) {
}
