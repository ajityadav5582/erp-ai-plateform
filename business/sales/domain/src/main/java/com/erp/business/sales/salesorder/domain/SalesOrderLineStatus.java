package com.erp.business.sales.salesorder.domain;

/**
 * Per-line delivery progress of a sales order line.
 *
 * <p>Derived from the delivered quantity by
 * {@link SalesOrderLine#recordDelivery(java.math.BigDecimal)} and reversed by
 * {@link SalesOrderLine#reverseDelivery(java.math.BigDecimal)}, so the client never
 * has to guess it.
 */
public enum SalesOrderLineStatus {

    /** Nothing delivered yet. */
    OPEN,

    /** Some quantity delivered, more still outstanding. */
    PARTIALLY_DELIVERED,

    /** Fully delivered. */
    DELIVERED,

    /** Line voided before delivery. */
    CANCELLED
}
