package com.erp.business.inventory.domain.report;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for the stock on-hand report: one row per (warehouse, item, unit)
 * with the item/warehouse/unit display fields resolved, so the report needs no
 * client-side lookups. A projection of {@code StockBalance} joined to the item,
 * warehouse and unit masters.
 */
public record StockOnHandView(
        Long itemId,
        String sku,
        String itemName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long unitId,
        String unitCode,
        BigDecimal onHand,
        BigDecimal reserved,
        BigDecimal available,
        Instant lastMovementAt
) {
}
