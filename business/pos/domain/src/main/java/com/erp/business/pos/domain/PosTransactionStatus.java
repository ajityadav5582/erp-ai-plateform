package com.erp.business.pos.domain;

/**
 * Lifecycle status of a POS transaction (sale).
 *
 * <p>A transaction moves through states exactly once in the forward direction.
 * The only allowed backward move is {@link #OPEN} → {@link #VOIDED} before
 * stock is deducted, or {@link #COMPLETED} → {@link #VOIDED} (with a stock
 * reversal). A {@link #VOIDED} transaction is terminal and immutable.
 *
 * <pre>
 *   OPEN ──────────────► COMPLETED
 *    │                       │
 *    └──────► VOIDED ◄───────┘
 * </pre>
 */
public enum PosTransactionStatus {

    /**
     * Items are being added to the cart; payment has not been taken.
     * Stock has not yet been deducted.
     */
    OPEN,

    /**
     * Payment accepted; stock deducted from the warehouse; CBMS sync pending
     * or complete. Terminal state unless subsequently voided.
     */
    COMPLETED,

    /**
     * Transaction was cancelled before or after completion.
     * If it was {@link #COMPLETED}, the stock deduction has been reversed.
     * Terminal state — immutable.
     */
    VOIDED
}
