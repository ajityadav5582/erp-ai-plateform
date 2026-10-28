package com.erp.business.inventory.domain.report;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for the low-stock / reorder report: one row per (warehouse, item, unit)
 * whose available quantity has fallen to or below the item's reorder threshold,
 * carrying the item's reorder policy and the suggested order quantity.
 *
 * <p>A projection of {@code Item} left-joined to {@code StockBalance}, so an item
 * that has never been stocked still appears with zero quantities.
 */
public record LowStockView(
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
        BigDecimal minStockLevel,
        BigDecimal reorderLevel,
        BigDecimal reorderQuantity,
        BigDecimal shortfall,
        BigDecimal suggestedOrderQty,
        String status,
        Instant lastMovementAt
) {

    /** Nothing left to sell or issue. */
    public static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    /** Below the safety-stock floor: replenish immediately. */
    public static final String STATUS_BELOW_MIN = "BELOW_MIN";

    /** At or below the reorder level: place a replenishment order. */
    public static final String STATUS_BELOW_REORDER = "BELOW_REORDER";
}
