package com.erp.business.pos.transaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One requested cart line: an item, a quantity and an optional line discount.
 *
 * <p>The unit price is never supplied by the caller — it is resolved server-side
 * from the Inventory catalogue, both for the price preview and again at checkout.
 *
 * @param itemId            the inventory item being sold
 * @param quantity          how many units; must be greater than zero
 * @param discountPercentage line discount percentage, 0–100; null means none
 */
public record CheckoutLineRequest(

        @NotNull(message = "Item is required")
        Long itemId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.00", message = "Discount percentage must not be negative")
        @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
        BigDecimal discountPercentage
) {
}
