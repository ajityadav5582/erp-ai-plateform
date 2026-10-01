package com.erp.business.procurement.purchaseorder.domain;

/**
 * Lifecycle state of a purchase order.
 *
 * <p>The permitted transitions are declared once here and enforced in
 * {@link PurchaseOrder#transitionTo(PurchaseOrderStatus, String, String)}. The
 * database {@code chk_purchase_orders_status} constraint only guards the value
 * set, not the transitions, because SQL cannot express "a PO may not move from
 * CLOSED back to DRAFT" without a trigger; the aggregate owns that rule.
 *
 * <pre>
 *   DRAFT -submit-> SUBMITTED -approve-> APPROVED -> PARTIALLY_RECEIVED -> RECEIVED -close-> CLOSED
 *                        |                         |
 *                        -reject-> REJECTED          -cancel-> CANCELLED
 * </pre>
 */
public enum PurchaseOrderStatus {

    /** Editable. Lines can be added, changed and removed freely. */
    DRAFT,

    /** Awaiting approval. Content is frozen; only approval, rejection or cancellation remain. */
    SUBMITTED,

    /** Issued to the supplier. Awaiting or partially through goods receipt. */
    APPROVED,

    /** Some lines received. The header total is unchanged; received_amount tracks progress. */
    PARTIALLY_RECEIVED,

    /** Every line fully received. Awaiting three-way match and settlement. */
    RECEIVED,

    /** Approval refused. Terminal; the PO must be re-raised as a new DRAFT. */
    REJECTED,

    /** Voided before or during receipt. Terminal. */
    CANCELLED,

    /** Fully received, matched and settled. Terminal and immutable. */
    CLOSED;

    /**
     * Returns whether a transition from this state to {@code target} is allowed.
     *
     * @param target the requested next state
     * @return {@code true} when the transition is part of the lifecycle
     */
    public boolean canTransitionTo(PurchaseOrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case DRAFT -> target == SUBMITTED || target == CANCELLED;
            case SUBMITTED -> target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED -> target == PARTIALLY_RECEIVED
                    || target == RECEIVED
                    || target == CANCELLED
                    || target == CLOSED;
            case PARTIALLY_RECEIVED -> target == RECEIVED || target == CANCELLED || target == CLOSED;
            case RECEIVED -> target == CLOSED || target == CANCELLED;
            case REJECTED, CANCELLED, CLOSED -> false;
        };
    }

    /**
     * Returns whether this state still permits line edits.
     *
     * @return {@code true} only for {@link #DRAFT}
     */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /**
     * Returns whether this state is final, meaning the document can never change again.
     *
     * @return {@code true} for terminal states
     */
    public boolean isTerminal() {
        return this == REJECTED || this == CANCELLED || this == CLOSED;
    }
}
