package com.erp.business.procurement.purchaserequisition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequisitionRequest(

        @NotBlank(message = "A cancellation reason is required")
        @Size(max = 500)
        String reason
) {
}
