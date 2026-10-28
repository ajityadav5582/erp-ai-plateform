package com.erp.business.procurement.workflow;

import com.erp.business.procurement.workflow.dto.WorkflowConfigurationResponse;

public interface WorkflowConfigurationService {

    /**
     * The company's workflow settings, or the safe default when unconfigured.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @return the effective settings; never null, never a partially populated object
     */
    WorkflowConfigurationResponse get(Long tenantId, Long companyId);

    /**
     * Replaces the company's workflow settings, creating the row on first write.
     *
     * <p>Applies to requisitions submitted after the change. A requisition already
     * sitting in an approval queue keeps the chain it was submitted with, because
     * the chain is snapshotted onto the document precisely so that changing a
     * policy mid-flight cannot retroactively move or drop a step.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param request   the new settings
     * @return the stored settings
     */
    WorkflowConfigurationResponse update(Long tenantId, Long companyId,
                                         WorkflowConfigurationResponse request);

    /**
     * Whether a requisition must be approved by a person in this company.
     *
     * <p>The single question the requisition workflow asks. Returns {@code true}
     * when the company is unconfigured, so a company that has never opened the
     * settings screen keeps its spend control.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @return {@code true} when the approval chain applies
     */
    boolean isPurchaseRequisitionApprovalRequired(Long tenantId, Long companyId);
}
