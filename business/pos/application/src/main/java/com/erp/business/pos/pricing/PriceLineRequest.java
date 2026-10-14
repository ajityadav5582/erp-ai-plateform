package com.erp.business.pos.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * One requested cart line for a price preview: an item, a quantity and an
 * optional line discount percentage. The unit price is never supplied by the
 * caller — it is resolved server-side from the catalogue.
 */
public record PriceLineRequest(

        @NotNull(message = "Item is required")
        Long itemId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @PositiveOrZero(message = "Discount percentage must not be negative")
        BigDecimal discountPercentage
) {
}
