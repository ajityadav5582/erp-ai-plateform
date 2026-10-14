package com.erp.business.sales.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SalesQuotationLineRequest(
        @NotNull(message = "Item ID is required")
        Long itemId,

        @NotBlank(message = "UOM code is required")
        String uomCode,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0", message = "Unit price must not be negative")
        BigDecimal unitPrice,

        BigDecimal discountPercentage,

        BigDecimal taxRate,

        String description
) {
}
