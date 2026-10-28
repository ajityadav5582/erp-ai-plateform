package com.erp.business.sales.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One line of a create-delivery request: a quantity shipped against a sales-order
 * line. The item is referenced by id; the service freezes its display fields.
 */
public record DeliveryLineRequest(

        @NotNull(message = "Sales order line number is required")
        @Positive(message = "Sales order line number must be positive")
        Integer salesOrderLineNumber,

        @NotNull(message = "Item is required")
        Long itemId,

        @NotBlank(message = "Unit of measure is required")
        @Size(max = 32, message = "UOM must not exceed 32 characters")
        String uomCode,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
        @Digits(integer = 13, fraction = 6, message = "Quantity has too many digits")
        BigDecimal quantity,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
