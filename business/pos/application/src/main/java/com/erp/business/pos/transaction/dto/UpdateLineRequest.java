package com.erp.business.pos.transaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Line edit on a parked (OPEN) cart: change the quantity and/or the discount of
 * one line. At least one of the two fields must be present; a {@code null}
 * field leaves that value unchanged.
 *
 * @param quantity           replacement quantity; must be greater than zero
 * @param discountPercentage replacement discount percentage, 0–100
 */
public record UpdateLineRequest(

        @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.00", message = "Discount percentage must not be negative")
        @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
        BigDecimal discountPercentage
) {
}
