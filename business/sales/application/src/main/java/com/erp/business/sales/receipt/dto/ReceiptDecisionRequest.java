package com.erp.business.sales.receipt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for a receipt decision that requires a reason (cancel).
 */
public record ReceiptDecisionRequest(

        @NotBlank(message = "A reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
