package com.erp.business.inventory.domain;

/**
 * The kind of physical movement a stock ledger entry records.
 *
 * <p>Sign convention: IN and RELEASE increase on-hand; OUT and RESERVE decrease on-hand.
 * ADJUST carries a signed quantity - positive adds, negative removes.
 */
public enum StockMovementType {

    /** Goods received. Increases on-hand. */
    IN,

    /** Goods issued or consumed. Decreases on-hand. */
    OUT,

    /** Manual correction. Signed: positive adds, negative removes. */
    ADJUST,

    /** Set aside for an outgoing order. Moves quantity into reserved. */
    RESERVE,

    /** Release previously reserved quantity back to available. */
    RELEASE
}
