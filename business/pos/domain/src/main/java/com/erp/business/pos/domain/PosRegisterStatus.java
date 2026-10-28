package com.erp.business.pos.domain;

/**
 * Status of a POS cash register terminal.
 *
 * <p>A register must be {@link #ACTIVE} for sessions to be opened on it.
 * Registers are never deleted — they are deactivated so historical sessions
 * remain fully traceable.
 */
public enum PosRegisterStatus {

    /** Register is operational and can have sessions opened on it. */
    ACTIVE,

    /** Register has been deactivated — no new sessions may be opened. */
    INACTIVE
}
