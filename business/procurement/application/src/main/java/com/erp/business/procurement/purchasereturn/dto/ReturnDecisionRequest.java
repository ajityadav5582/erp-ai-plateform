package com.erp.business.procurement.purchasereturn.dto;

import jakarta.validation.constraints.NotBlank;

public record ReturnDecisionRequest(
        @NotBlank(message = "A decision reason is required")
        String reason
) {}

