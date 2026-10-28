package com.erp.business.procurement.purchaseorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the reason for refusing or voiding a purchase order.
 *
 * <p>A separate body because both transitions demand one. An unexplained rejection is
 * not auditable: the supplier asks why, the buyer cannot answer, and the record only
 * says the order was refused.
 */
public record PurchaseOrderDecisionRequest(

        @NotBlank(message = "A reason is required")
        @Size(max = 500)
        String reason
) {
}
