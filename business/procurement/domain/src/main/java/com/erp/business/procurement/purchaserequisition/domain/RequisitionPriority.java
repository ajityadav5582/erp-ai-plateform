package com.erp.business.procurement.purchaserequisition.domain;

/**
 * Business urgency of a requisition.
 *
 * <p>Priority does not change the approval chain, which is driven by amount
 * bands in {@code PurchaseRequisitionApprovalPolicy}. It drives queue ordering
 * and, in most organisations, delivery-date promises. Keeping the two
 * independent matters: a small urgent request must still be approved by the
 * same authority as a large routine one, otherwise priority becomes a way to
 * bypass financial control.
 *
 * <p>Explicitly an enum rather than a numeric severity. A free integer invites
 * the value 7 to sort above {@link #URGENT} without anyone noticing, and it
 * cannot be rendered meaningfully in a UI.
 *
 * @since 1.0.0
 */
public enum RequisitionPriority {

    /** Planned demand. Queued normally. */
    LOW,

    /** Standard business demand. The default. */
    NORMAL,

    /** Ahead of routine demand in every queue. */
    HIGH,

    /**
     * Production-stopping or safety-critical. Must still be justified in the
     * {@code justification} field.
     */
    URGENT;

    /**
     * Returns the numeric weight used for queue ordering.
     *
     * <p>Derived from the declaration order rather than stored, so reordering
     * the constants changes every weight consistently instead of leaving stale
     * literals behind.
     *
     * @return a strictly increasing weight, lowest priority first
     */
    public int weight() {
        return ordinal();
    }
}
