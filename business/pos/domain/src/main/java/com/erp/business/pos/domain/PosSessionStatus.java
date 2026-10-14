package com.erp.business.pos.domain;

/**
 * Status of a cashier shift (session) on a POS register.
 *
 * <p>Only one session per register may be {@link #OPEN} at a time — enforced
 * by a partial unique index on the {@code pos_sessions} table and by an
 * application-level guard in the service layer.
 */
public enum PosSessionStatus {

    /** Session is active; transactions may be created under it. */
    OPEN,

    /** Session has been closed by the cashier; no further transactions allowed. */
    CLOSED
}
