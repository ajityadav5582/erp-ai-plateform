package com.erp.business.inventory.report.dto;

import java.math.BigDecimal;

/** One row of the period movement summary: opening + in − out ± adjust = closing. */
public record StockMovementSummaryRow(
        Long itemId,
        String sku,
        String itemName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long unitId,
        String unitCode,
        BigDecimal opening,
        BigDecimal totalIn,
        BigDecimal totalOut,
        BigDecimal totalAdjust,
        BigDecimal closing
) {
}
