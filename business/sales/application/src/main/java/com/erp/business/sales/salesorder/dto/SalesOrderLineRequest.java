package com.erp.business.sales.salesorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One line of a create/update sales-order request.
 *
 * <p>The item is referenced by id; the service resolves and freezes its display
 * fields onto the persisted line, so the client never supplies the snapshot.
 */
public record SalesOrderLineRequest(

        @NotNull(message = "Item is required")
        Long itemId,

        @NotBlank(message = "Unit of measure is required")
        @Size(max = 32, message = "UOM must not exceed 32 characters")
        String uomCode,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
        @Digits(integer = 13, fraction = 6, message = "Quantity has too many digits")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0", message = "Unit price must not be negative")
        @Digits(integer = 13, fraction = 4, message = "Unit price has too many digits")
        BigDecimal unitPrice,

        @DecimalMin(value = "0", message = "Discount percentage must not be negative")
        @Digits(integer = 3, fraction = 2, message = "Discount percentage has too many digits")
        BigDecimal discountPercentage,

        @DecimalMin(value = "0", message = "Tax rate must not be negative")
        @Digits(integer = 3, fraction = 2, message = "Tax rate has too many digits")
        BigDecimal taxRate,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
