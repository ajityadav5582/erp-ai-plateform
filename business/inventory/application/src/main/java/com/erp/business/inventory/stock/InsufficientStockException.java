package com.erp.business.inventory.stock;

import java.math.BigDecimal;

/**
 * Thrown when an outbound stock movement would take a warehouse's on-hand
 * quantity below zero and the warehouse does not allow negative stock.
 *
 * <p>Mapped to HTTP 422 by the stock-movement controller so callers (POS
 * checkout, sales delivery) can show the operator exactly which item is short
 * and by how much, rather than a generic server error.
 */
public class InsufficientStockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String itemSku, String warehouseCode,
                                      BigDecimal requested, BigDecimal onHand) {
        super(String.format(
                "Insufficient stock for item %s at %s: requested %s, on hand %s",
                itemSku, warehouseCode, requested, onHand));
    }
}
