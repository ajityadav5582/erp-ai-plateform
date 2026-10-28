package com.erp.business.procurement.purchaserequisition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRequisitionLineRequest(

        @NotNull(message = "Item is required")
        Long itemId,

        @NotBlank(message = "Item code is required")
        @Size(max = 100)
        String itemCode,

        @NotBlank(message = "Item name is required")
        @Size(max = 255)
        String itemName,

        @NotBlank(message = "Unit of measure is required")
        @Size(max = 32)
        String uomCode,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        @Digits(integer = 13, fraction = 6)
        BigDecimal quantity,

        @NotNull(message = "Estimated unit price is required")
        @DecimalMin(value = "0.00", message = "Estimated unit price cannot be negative")
        @Digits(integer = 15, fraction = 4)
        BigDecimal estimatedUnitPrice,

        @NotNull(message = "Tax rate is required")
        @DecimalMin(value = "0.00", message = "Tax rate cannot be negative")
        @DecimalMax(value = "100.00", message = "Tax rate cannot exceed 100")
        BigDecimal taxRate,

        @Size(max = 1000)
        String remarks
) {
}
