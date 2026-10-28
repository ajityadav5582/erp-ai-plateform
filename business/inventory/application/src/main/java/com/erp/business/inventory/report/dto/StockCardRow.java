package com.erp.business.inventory.report.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One line of a stock card: a movement plus its effect and the running balance. */
public record StockCardRow(
        Instant movementDate,
        String movementType,
        String referenceType,
        String referenceNumber,
        BigDecimal quantity,
        BigDecimal onHandDelta,
        BigDecimal runningBalance,
        String postedBy,
        String notes,
        String warehouseCode,
        String warehouseName,
        String unitCode
) {
}
