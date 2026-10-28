package com.erp.business.sales.salesorder.domain;

/**
 * Lifecycle state of a sales order.
 *
 * <p>The permitted transitions are declared once here and enforced in
 * {@link SalesOrder#transitionTo(SalesOrderStatus, String, String)}. The database
 * check constraint only guards the value set, not the transitions, because SQL
 * cannot express "an order may not move from CLOSED back to DRAFT" without a
 * trigger; the aggregate owns that rule.
 *
 * <pre>
 *   DRAFT --confirm--> CONFIRMED --deliveries--> DELIVERED --close--> CLOSED
 *     |                    |                          ^
 *     |                    +--partial--> PARTIALLY_DELIVERED
 *     +---cancel---------> CANCELLED (from DRAFT or CONFIRMED)
 * </pre>
 *
 * <p>Delivery progress is not a hand-picked transition: it is derived from the
 * lines by {@link SalesOrder#deliverAgainstLine(int, java.math.BigDecimal)}, which
 * moves the order to {@link #PARTIALLY_DELIVERED} or {@link #DELIVERED} depending
 * on how much of the order is still outstanding, and back again when a delivery is
 * reversed.
 */
public enum SalesOrderStatus {

    /** Editable. Lines can be added, changed and removed freely. */
    DRAFT,

    /** Issued to the customer. Awaiting or partially through delivery. */
    CONFIRMED,

    /** Some quantity delivered, more still outstanding. */
    PARTIALLY_DELIVERED,

    /**
     * Every counted line fully delivered. Derived from the lines, never set
     * directly: an order reaches this state only when nothing is outstanding.
     */
    DELIVERED,

    /** Voided before delivery completed. Terminal. */
    CANCELLED,

    /** Delivery complete and the order settled administratively. Terminal. */
    CLOSED
}
