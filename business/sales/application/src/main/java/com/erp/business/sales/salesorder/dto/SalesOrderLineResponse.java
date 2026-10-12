package com.erp.business.sales.salesorder.dto;

import com.erp.business.sales.salesorder.domain.SalesOrderLineStatus;

import java.math.BigDecimal;

/**
 * Read model for one sales-order line.
 */
public record SalesOrderLineResponse(
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
        BigDecimal deliveredQuantity,
        BigDecimal outstandingQuantity,
        SalesOrderLineStatus status,
        String description
) {
}
