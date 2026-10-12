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
 *   DRAFT --submit--> SUBMITTED --approve--> APPROVED --receipts--> RECEIVED --close--> CLOSED
 *     |                   |                     |  \                   ^
 *     |                   |--reject--> REJECTED |   \--cancel--> CANCELLED
 *     |                   |                     |     |
 *     +---cancel---------> CANCELLED           |     +--cancel--> CANCELLED
 *                                                 |
 *                            APPROVED <--partial--- PARTIALLY_RECEIVED
 * </pre>
 *
 * <p>Receipt progress is not a hand-picked transition: it is derived from the lines
 * by {@link PurchaseOrder#receiveAgainstLine(int, java.math.BigDecimal)}, which
 * moves the order to {@link #PARTIALLY_RECEIVED} or {@link #RECEIVED} depending on
 * how much of the order is still outstanding, and back again when a voided goods
 * receipt is reversed.
 */
public enum PurchaseOrderStatus {

    /** Editable. Lines can be added, changed and removed freely. */
    DRAFT,

    /** Awaiting approval. Content is frozen; only approval, rejection or cancellation remain. */
    SUBMITTED,

    /** Issued to the supplier. Awaiting or partially through goods receipt. */
    APPROVED,

    /**
     * Some quantity received, more still outstanding.
     *
     * <p>Covers both shapes of shortfall that occur in practice: lines that have
     * not been delivered at all, and lines that arrived short. The header total is
     * unchanged; {@code received_amount} tracks progress.
     */
    PARTIALLY_RECEIVED,

    /**
     * Every counted line fully received. Awaiting three-way match and settlement.
     *
     * <p>Derived from the lines, never set directly: an order reaches this state
     * only when nothing is outstanding, which is what makes it safe to close and
     * unsafe to receive against.
     */
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
     * <p><strong>{@code CLOSED} is reachable only from {@code RECEIVED}.</strong>
     * Closing means matched and settled, so an order still owing goods cannot be
     * closed: doing so retired the document while deliveries were still arriving
     * and every subsequent receipt was then refused. The quantity check itself
     * lives in {@link PurchaseOrder}, which is where the lines are; this table only
     * refuses the states that are structurally incapable of being settled.
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
                    || target == CANCELLED;
            case PARTIALLY_RECEIVED -> target == RECEIVED || target == CANCELLED;
            case RECEIVED -> target == CLOSED || target == CANCELLED;
            case REJECTED, CANCELLED, CLOSED -> false;
        };
    }

    /**
     * Returns whether this state permits goods receipts to be posted against the
     * order.
     *
     * <p>The answer the receiving screen needs before it offers to book a delivery:
     * an order is receivable from the moment it is issued to the supplier until
     * every line has arrived, inclusive. {@link #RECEIVED} is excluded because
     * there is by then nothing left to book.
     *
     * @return {@code true} for {@link #APPROVED} and {@link #PARTIALLY_RECEIVED}
     */
    public boolean isReceivable() {
        return this == APPROVED || this == PARTIALLY_RECEIVED;
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
