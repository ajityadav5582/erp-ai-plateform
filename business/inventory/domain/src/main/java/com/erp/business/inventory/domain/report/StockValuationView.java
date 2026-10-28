package com.erp.business.inventory.domain.report;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model for the stock valuation report: one row per (warehouse, item, unit)
 * carrying a non-zero quantity, valued at the item's purchase price.
 *
 * <p>This is an <em>indicative</em> valuation. The unit cost is the item master's
 * purchase price, not the cost actually paid on the movement that brought the
 * stock in, so it will not tie to a perpetual inventory ledger until movements
 * carry their own unit cost.
 */
public record StockValuationView(
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
        BigDecimal unitCost,
        BigDecimal onHandValue,
        BigDecimal reservedValue,
        BigDecimal availableValue,
        boolean costBasisMissing,
        Instant lastMovementAt
) {
}
