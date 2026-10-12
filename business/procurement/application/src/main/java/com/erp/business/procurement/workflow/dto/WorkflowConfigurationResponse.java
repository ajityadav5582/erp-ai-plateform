package com.erp.business.procurement.workflow.dto;

import jakarta.validation.constraints.NotNull;

/**
 * The company's workflow switches, as the settings screen sees them.
 *
 * <p>Read and write use the same shape on purpose. The screen renders these
 * values into a form and posts the form straight back, so a separate request
 * record would mean every field is declared and mapped twice, and the two
 * declarations would eventually disagree about which fields exist.
 *
 * @param purchaseRequisitionApprovalRequired whether a requisition must be
 *        approved by a person before it can become a purchase order
 */
public record WorkflowConfigurationResponse(
        @NotNull(message = "purchaseRequisitionApprovalRequired is required")
        Boolean purchaseRequisitionApprovalRequired
) {
}
