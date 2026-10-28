package com.erp.business.procurement.purchaserequisition.domain;

/**
 * Decision recorded against one step of a requisition approval chain.
 *
 * <p>The chain is materialised onto the requisition at submission time as a set
 * of {@code PurchaseRequisitionApproval} rows. This enum is the vocabulary for
 * those rows' {@code action} column.
 *
 * <p>{@link #SKIPPED} exists because amount-band policies are evaluated
 * sequentially: a step whose threshold does not apply to this document's value
 * is marked skipped rather than deleted, so the chain still reads as the full
 * policy that was applied. Deleting inapplicable steps would leave an audit
 * reader unable to tell a skip from a missing configuration.
 *
 * @since 1.0.0
 */
public enum ApprovalAction {

    /** Awaiting a decision. The only state in which the step is still open. */
    PENDING,

    /** Step approved. The chain may advance to the next step. */
    APPROVED,

    /** Approval refused. Terminal for the whole document; the demand must be re-raised. */
    REJECTED,

    /** The step's threshold did not apply to this document. No decision needed. */
    SKIPPED;

    /**
     * Returns whether this action closes the step.
     *
     * @return {@code true} for every action except {@link #PENDING}
     */
    public boolean isDecided() {
        return this != PENDING;
    }

    /**
     * Returns whether this action ends the requisition.
     *
     * <p>Only a refusal does. There is no "send it back for rework" outcome on
     * this document: a requisition that an approver does not accept is rejected,
     * and anything the requester still wants to buy is re-raised as a new draft.
     * That keeps the workflow free of a state in which the same document has been
     * approved in one form and is live for editing in another.
     *
     * @return {@code true} only for {@link #REJECTED}
     */
    public boolean isTerminal() {
        return this == REJECTED;
    }
}
