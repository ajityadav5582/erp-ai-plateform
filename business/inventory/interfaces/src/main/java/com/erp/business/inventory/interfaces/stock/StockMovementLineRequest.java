package com.erp.business.inventory.interfaces.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One line of a stock movement batch: a single item landing in (or leaving) a
 * warehouse. The unit travels as its <strong>code</strong>, not an id, because
 * Inventory owns the unit master and a balance is held per (warehouse, item,
 * unit) - the same item in pieces and in boxes are two different balances.
 * Sending the code lets Inventory resolve it within the caller's own
 * tenant/company, so a caller never has to know another scope's unit id.
 *
 * @param warehouseId the warehouse the goods move through
 * @param itemId      the item being moved
 * @param uomCode     the unit-of-measure code, resolved by Inventory in scope
 * @param quantity    how much moved; positive for IN/OUT, signed for ADJUST
 */
public record StockMovementLineRequest(
        @NotNull(message = "Warehouse is required") Long warehouseId,
        @NotNull(message = "Item is required") Long itemId,
        @NotBlank(message = "Unit of measure code is required") String uomCode,
        @NotNull(message = "Quantity is required") BigDecimal quantity
) {
}
