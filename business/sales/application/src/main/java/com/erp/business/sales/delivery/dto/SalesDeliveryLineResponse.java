package com.erp.business.sales.delivery.dto;

import java.math.BigDecimal;

/**
 * Read model for one sales-delivery line.
 */
public record SalesDeliveryLineResponse(
        Long id,
        Integer lineNumber,
        Integer salesOrderLineNumber,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal quantity,
        String description
) {
}
