package com.erp.business.procurement.stock.domain;

/**
 * The kind of physical movement a stock ledger entry records.
 *
 * <p>These are the four movements the inbound warehouse performs. They are kept as an
 * enum rather than free text because every caller has to pick one: a movement that is
 * neither in nor out nor an adjustment is not a movement, and forcing the choice at the
 * request boundary is how a typo becomes a compile failure instead of a corrupt balance.
 *
 * <p><strong>Sign convention.</strong> {@link #IN} and {@link #OUT} carry an absolute
 * positive quantity; the sign is decided by the type, so a caller cannot post a negative
 * receipt. {@link #ADJUST} carries a signed quantity - positive to add, negative to
 * remove - because a correction is explicitly a move in either direction and saying so
 * is more honest than inventing an OUT for stock that was never issued.
 */
public enum StockMovementType {

    /** Goods received. Increases on-hand. */
    IN,

    /** Goods issued or reversed. Decreases on-hand. */
    OUT,

    /** Manual correction. Signed: positive adds, negative removes. */
    ADJUST,

    /** Set aside for an outgoing order. Moves quantity into reserved. */
    RESERVE,

    /** Release previously reserved quantity back to available. */
    RELEASE
}
