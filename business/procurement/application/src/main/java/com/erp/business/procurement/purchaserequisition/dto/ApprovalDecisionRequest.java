package com.erp.business.procurement.purchaserequisition.dto;

import com.erp.business.procurement.purchaserequisition.domain.ApprovalAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * One approving decision, carrying everything an approver can say in a single
 * submission.
 *
 * <p>The three line-level collections are deliberately separate rather than one
 * tagged union, because they answer different questions: {@code lineApprovals}
 * says how much of each line is granted, {@code rejectedLineIds} says which
 * lines the approver refuses outright, and {@code cancelledLineIds} says which
 * are being withdrawn. A caller must not be able to approve and reject the same
 * line in one request, so the service proves the three sets are disjoint before
 * applying any of them.
 */
public record ApprovalDecisionRequest(

        @NotNull(message = "An approval decision is required")
        ApprovalAction action,

        /**
         * Per-line grants. An omitted line is approved in full; an entry below the
         * requested quantity records a partial approval. A grant above the request
         * is rejected by the aggregate.
         */
        List<LineApproval> lineApprovals,

        /**
         * Lines refused outright, without voiding the rest of the document.
         *
         * <p>A branch asking for ten items should not lose the three that are
         * genuinely needed because one was disallowed, so this is the difference
         * between a usable approval screen and one where disallowing anything means
         * sending the whole request back to the requester to be retyped.
         *
         * <p>Only honoured on an approving decision. On a rejection the whole
         * document is already ending, and marking individual lines as refused as
         * well would record the same outcome twice in the register.
         */
        @Size(max = 200, message = "Too many rejected lines in one decision")
        List<Long> rejectedLineIds,

        /**
         * Lines withdrawn: no longer needed, as opposed to disallowed.
         *
         * <p>The distinction is for the audit trail rather than the arithmetic -
         * both stop the line contributing to the totals - because "the approver
         * said no" and "we no longer need this" are different facts and a reader
         * of the register has to be able to tell them apart.
         */
        @Size(max = 200, message = "Too many cancelled lines in one decision")
        List<Long> cancelledLineIds,

        /**
         * Required for rejection and return, enforced in the aggregate so the
         * requester always receives something actionable.
         */
        @Size(max = 500)
        String comments
) {
    public record LineApproval(
            @NotNull(message = "Line is required")
            Long lineId,

            @NotNull(message = "Granted quantity is required")
            BigDecimal approvedQuantity
    ) {
    }
}
