package com.erp.business.procurement.purchaserequisition.domain;

/**
 * Lifecycle state of a branch purchase requisition.
 *
 * <p>The permitted transitions are declared once here and enforced by the named
 * business methods on {@code PurchaseRequisition} - {@code submit()},
 * {@code approve()}, {@code reject()}, {@code cancel()} and {@code convertToPO()}.
 * There is deliberately no public setter and no generic {@code transitionTo(...)}:
 * a status change is a business event with its own preconditions, its own
 * validation and its own workflow stamps, and a method that accepts an arbitrary
 * target cannot enforce any of that.
 *
 * <p>A column-level constraint can only guard the value set, not the transitions,
 * because SQL cannot express "an approved requisition may never return to draft"
 * without a trigger. The aggregate therefore owns the transition rule.
 *
 * <pre>
 *   DRAFT --submit--> SUBMITTED --approve--> APPROVED --convertToPO--> CONVERTED_TO_PO
 *     |                  |                        |
 *     |                  |--reject--> REJECTED    |
 *     |                  |                        |
 *     +----cancel--------+--cancel---------------+
 * </pre>
 *
 * <p>Terminal states: {@link #REJECTED}, {@link #CANCELLED},
 * {@link #CONVERTED_TO_PO}. A requisition converts to a purchase order once,
 * because conversion is what commits approved demand to a supplier; how much was
 * converted is answered by the line quantities and the order links rather than by
 * a header flag.
 *
 * @since 1.0.0
 */
public enum PurchaseRequisitionStatus {

    /** Editable. Lines can be added, changed and removed freely. */
    DRAFT,

    /** Submitted and awaiting an approval decision. Content is frozen. */
    SUBMITTED,

    /** Fully approved. Demand is now a procurement commitment-in-waiting. */
    APPROVED,

    /** Approval refused. Terminal; the demand must be re-raised as a new draft. */
    REJECTED,

    /** Withdrawn by the requester or an administrator. Terminal. */
    CANCELLED,

    /** Approved demand has been converted into purchase orders. Terminal. */
    CONVERTED_TO_PO;

    /**
     * Returns whether a transition from this state to {@code target} is allowed.
     *
     * <p>This is the transition table, and it is deliberately the only place the
     * permitted edges are written down. Each business method calls it for its own
     * target before applying anything, so adding an edge here without adding the
     * matching method would be a route to nowhere.
     *
     * @param target the requested next state
     * @return {@code true} when the transition is part of the lifecycle
     */
    public boolean canTransitionTo(PurchaseRequisitionStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case DRAFT -> target == SUBMITTED || target == CANCELLED;
            case SUBMITTED -> target == APPROVED
                    || target == REJECTED
                    || target == CANCELLED;
            case APPROVED -> target == CONVERTED_TO_PO;
            case REJECTED, CANCELLED, CONVERTED_TO_PO -> false;
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
     * Returns whether this state is awaiting an approval decision.
     *
     * @return {@code true} only for {@link #SUBMITTED}
     */
    public boolean isAwaitingApproval() {
        return this == SUBMITTED;
    }

    /**
     * Returns whether approved demand may still be converted into a purchase
     * order.
     *
     * @return {@code true} only for {@link #APPROVED}
     */
    public boolean isConvertibleToPurchaseOrder() {
        return this == APPROVED;
    }

    /**
     * Returns whether this state consumes branch budget.
     *
     * <p>Budget is committed at approval, not at conversion: approval is the point
     * at which the company commits to fund the demand, and converting it to a
     * purchase order does not change that fact.
     *
     * @return {@code true} for the approved and converted states
     */
    public boolean isBudgetConsuming() {
        return this == APPROVED || this == CONVERTED_TO_PO;
    }

    /**
     * Returns whether this state is final, meaning the document can never change
     * again.
     *
     * @return {@code true} for terminal states
     */
    public boolean isTerminal() {
        return this == REJECTED || this == CANCELLED || this == CONVERTED_TO_PO;
    }
}
