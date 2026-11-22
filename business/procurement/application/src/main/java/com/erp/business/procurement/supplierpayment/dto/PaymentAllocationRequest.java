package com.erp.business.procurement.supplierpayment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Line-level allocation of money against a specific purchase invoice.
 */
public record PaymentAllocationRequest(

        @NotNull(message = "Purchase invoice ID is required")
        @Positive(message = "Purchase invoice ID must be positive")
        Long purchaseInvoiceId,

        @NotNull(message = "Allocated amount is required")
        @DecimalMin(value = "0.0001", message = "Allocated amount must be greater than zero")
        BigDecimal allocatedAmount,

        @DecimalMin(value = "0.0000", message = "Discount amount cannot be negative")
        BigDecimal discountAmount,

        @Size(max = 500)
        String notes
) {
}

