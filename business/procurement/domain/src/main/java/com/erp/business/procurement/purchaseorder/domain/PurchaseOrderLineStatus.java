package com.erp.business.procurement.purchaseorder.domain;

import java.math.BigDecimal;

/**
 * Execution state of a single purchase order line.
 *
 * <p>A line tracks its own state so a partially received document can be
 * filtered at line level without scanning the whole order, and so the goods
 * receiving worklist can select "what is still outstanding" with one indexed
 * query.
 */
public enum PurchaseOrderLineStatus {

    /** Nothing received yet. */
    OPEN,

    /** Some quantity received, more still outstanding. */
    PARTIALLY_RECEIVED,

    /** Ordered quantity fully received. */
    RECEIVED,

    /** Line withdrawn. Contributes nothing to the header totals. */
    CANCELLED;

    /**
     * Derives the line state from its received quantity.
     *
     * <p>Deriving instead of storing a free-form flag is what keeps
     * {@code received_quantity} and {@code status} from disagreeing.
     *
     * @param orderedQuantity  the ordered quantity, must be positive
     * @param receivedQuantity the cumulative received quantity, must not be negative
     * @return {@link #RECEIVED} when fully received, {@link #PARTIALLY_RECEIVED}
     *         when some quantity arrived, otherwise {@link #OPEN}
     * @throws IllegalArgumentException if either quantity is negative or the
     *                                  ordered quantity is not positive
     */
    public static PurchaseOrderLineStatus fromReceivedQuantity(
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity) {

        if (orderedQuantity == null || orderedQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Ordered quantity must be positive");
        }
        if (receivedQuantity == null || receivedQuantity.signum() < 0) {
            throw new IllegalArgumentException("Received quantity cannot be negative");
        }
        if (receivedQuantity.compareTo(orderedQuantity) >= 0) {
            return RECEIVED;
        }
        if (receivedQuantity.signum() > 0) {
            return PARTIALLY_RECEIVED;
        }
        return OPEN;
    }
}
