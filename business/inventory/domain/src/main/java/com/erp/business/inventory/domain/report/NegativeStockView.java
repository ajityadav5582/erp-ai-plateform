package com.erp.business.inventory.domain.report;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for the negative-stock report: one row per (warehouse, item, unit)
 * whose on-hand quantity has gone below zero.
 *
 * <p>A negative balance in a warehouse that permits negative stock is an
 * intentional backorder. The same balance in a warehouse that does not is a
 * data error — stock was issued without ever being received — and is what this
 * report exists to surface.
 */
public record NegativeStockView(
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
        boolean negativeStockAllowed,
        Instant lastMovementAt
) {
}
