package com.erp.business.procurement.purchaserequisition.dto;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;

import java.util.List;

/**
 * What the calling user may do with one requisition, and why not when they may
 * not.
 *
 * <p><strong>Why the server answers this rather than the client deriving it.</strong>
 * Every one of these flags depends on state the client does not have: which
 * approval step is currently pending, whether that step names this user or a role
 * they hold, which of the decision permissions their token carries, and which
 * ownership operations the calling user may perform on this particular document.
 * A client that guesses any of this renders buttons that 403 when pressed, which
 * is the specific complaint this response exists to remove.
 *
 * <p>The flags are advisory for rendering only. Every one of them is re-derived
 * and enforced on the endpoint that performs the action, because a UI hint that
 * can be forged by the caller is not a control.
 *
 * @param requisitionId  the requisition examined
 * @param status         its current lifecycle state
 * @param canEdit        whether its content may still be amended
 * @param canSubmit      whether the caller may submit it for approval
 * @param canDecide      whether the caller may record an approval decision
 * @param canCancel      whether the caller may cancel it
 * @param canDelete      whether the caller may delete it outright
 * @param canAwardToOrder whether the caller may award approved quantity to a PO
 * @param awaitingStepSequenceNo the step awaiting a decision, null when none is
 * @param blockers       human-readable reasons the actions above are unavailable
 */
public record RequisitionActionabilityResponse(
        Long requisitionId,
        PurchaseRequisitionStatus status,
        boolean canEdit,
        boolean canSubmit,
        boolean canDecide,
        boolean canCancel,
        boolean canDelete,
        boolean canAwardToOrder,
        Integer awaitingStepSequenceNo,
        List<String> blockers
) {

    public static RequisitionActionabilityResponse none(Long id, PurchaseRequisitionStatus status,
                                                       Integer awaitingStep, List<String> blockers) {
        return new RequisitionActionabilityResponse(
                id, status, false, false, false, false, false, false, awaitingStep, blockers);
    }
}
