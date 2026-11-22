package com.erp.business.procurement.supplierpayment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request to apply / offset unallocated advance credit from a supplier payment
 * against an approved purchase invoice.
 */
public record ApplyAdvanceRequest(

        @NotNull(message = "Advance payment ID is required")
        @Positive(message = "Advance payment ID must be positive")
        Long advancePaymentId,

        @NotNull(message = "Purchase invoice ID is required")
        @Positive(message = "Purchase invoice ID must be positive")
        Long purchaseInvoiceId,

        @NotNull(message = "Amount to apply is required")
        @DecimalMin(value = "0.0001", message = "Amount to apply must be greater than zero")
        BigDecimal amountToApply,

        @Size(max = 500)
        String notes
) {
}

