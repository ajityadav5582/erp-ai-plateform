package com.erp.business.sales.salesorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for a sales-order decision that requires a reason (cancel).
 */
public record SalesOrderDecisionRequest(

        @NotBlank(message = "A reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
