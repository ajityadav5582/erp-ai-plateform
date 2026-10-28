package com.erp.business.sales.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for an invoice decision that requires a reason (cancel).
 */
public record InvoiceDecisionRequest(

        @NotBlank(message = "A reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
