package com.erp.business.sales.delivery.domain;

/**
 * Lifecycle state of a sales delivery (goods issue).
 *
 * <pre>
 *   DRAFT --post--> POSTED --cancel--> CANCELLED
 * </pre>
 *
 * <p>Posting is the moment goods physically leave: it is what drives the outbound
 * stock movement in Inventory and the delivery progress on the sales order. A
 * posted delivery can only be cancelled, which reverses both.
 */
public enum DeliveryStatus {

    /** Editable. Lines can be added and removed freely. */
    DRAFT,

    /** Goods issued. Stock decremented and the order's delivery progress advanced. */
    POSTED,

    /** Voided after posting. Stock restored and the order's progress rolled back. */
    CANCELLED
}
