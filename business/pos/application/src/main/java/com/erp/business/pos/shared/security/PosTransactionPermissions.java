package com.erp.business.pos.shared.security;

/**
 * Permissions for POS transactions (checkout, void, refund).
 *
 * <p>Every code must have a matching row in Identity's {@code permissions} table,
 * seeded by {@code DataInitializer}; a code with no row can never be granted and
 * closes the annotated method to every caller.
 *
 * <p>{@link #POS_TRANSACTION_REFUND} is declared for the future returns flow.
 * Version 1 sells void-only: genuine post-shift returns are routed through the
 * existing Sales Return / credit-note flow, so no method carries the refund
 * permission yet.
 */
public final class PosTransactionPermissions {

    /** Read POS transactions and receipts. */
    public static final String POS_TRANSACTION_VIEW = "POS_TRANSACTION_VIEW";

    /** Complete a checkout: take payment, deduct stock, update the session. */
    public static final String POS_TRANSACTION_CHECKOUT = "POS_TRANSACTION_CHECKOUT";

    /** Void an open or completed transaction, reversing stock and session totals. */
    public static final String POS_TRANSACTION_VOID = "POS_TRANSACTION_VOID";

    /** Refund a completed transaction (reserved for the future returns flow). */
    public static final String POS_TRANSACTION_REFUND = "POS_TRANSACTION_REFUND";

    private PosTransactionPermissions() {
        throw new AssertionError("No instances");
    }
}
