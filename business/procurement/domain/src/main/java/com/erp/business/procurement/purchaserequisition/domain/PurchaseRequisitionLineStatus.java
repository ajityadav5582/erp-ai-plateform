package com.erp.business.procurement.purchaserequisition.domain;

import java.math.BigDecimal;

/**
 * Execution state of a single purchase requisition line.
 *
 * <p>A line carries its own state for the same reason purchase order lines do:
 * so that the procurement worklist can ask "what approved quantity is still
 * un-ordered?" with one indexed query instead of aggregating across documents.
 *
 * <p>Note that {@link #PARTIALLY_APPROVED} exists only here, not on the
 * purchase order line enum. Approving 60 of 100 requested units is normal and
 * expected in branch requisitions; it has no purchase-order analogue because a
 * purchase order line is only ever created for quantity already awarded.
 *
 * @since 1.0.0
 */
public enum PurchaseRequisitionLineStatus {

    /** Awaiting an approval decision. */
    REQUESTED,

    /** Fully approved; the full requested quantity was granted. */
    APPROVED,

    /** Approved for less than requested. The shortfall is visible for reporting. */
    PARTIALLY_APPROVED,

    /** Part of the approved quantity has been converted into purchase orders. */
    PARTIALLY_ORDERED,

    /** The whole approved quantity has been converted into purchase orders. */
    ORDERED,

    /** Approval refused on this specific line. Does not void the document. */
    REJECTED,

    /** Withdrawn. Contributes nothing to the header totals. */
    CANCELLED;

    /**
     * Derives the state from requested, approved and ordered quantities.
     *
     * <p>Deriving rather than storing a free-form flag is what keeps
     * {@code approved_quantity}, {@code ordered_quantity} and {@code status}
     * from drifting apart. A rejected or cancelled line is preserved as-is,
     * because those are decisions that must not be recomputed away.
     *
     * @param requestedQuantity the requested quantity, must not be negative
     * @param approvedQuantity  the approved quantity, must not be negative
     * @param orderedQuantity   the ordered quantity, must not be negative
     * @return the derived state
     * @throws IllegalArgumentException if any quantity is negative
     */
    public static PurchaseRequisitionLineStatus fromQuantities(
            BigDecimal requestedQuantity,
            BigDecimal approvedQuantity,
            BigDecimal orderedQuantity) {

        if (requestedQuantity == null || requestedQuantity.signum() < 0) {
            throw new IllegalArgumentException("Requested quantity cannot be negative");
        }
        if (approvedQuantity == null || approvedQuantity.signum() < 0) {
            throw new IllegalArgumentException("Approved quantity cannot be negative");
        }
        if (orderedQuantity == null || orderedQuantity.signum() < 0) {
            throw new IllegalArgumentException("Ordered quantity cannot be negative");
        }
        if (approvedQuantity.signum() == 0) {
            return REQUESTED;
        }
        if (orderedQuantity.compareTo(approvedQuantity) >= 0) {
            return ORDERED;
        }
        if (orderedQuantity.signum() > 0) {
            return PARTIALLY_ORDERED;
        }
        // No ordered quantity yet - check if fully or partially approved
        if (approvedQuantity.compareTo(requestedQuantity) < 0) {
            return PARTIALLY_APPROVED;
        }
        return APPROVED;
    }

    /**
     * Returns whether this line still contributes to the header totals.
     *
     * @return {@code false} for rejected and cancelled lines
     */
    public boolean isCounted() {
        return this != REJECTED && this != CANCELLED;
    }

    /**
     * Returns whether approved quantity on this line may still be ordered.
     *
     * @return {@code true} for {@link #APPROVED} and {@link #PARTIALLY_ORDERED}
     */
    public boolean isOrderable() {
        return this == APPROVED || this == PARTIALLY_ORDERED;
    }
}
