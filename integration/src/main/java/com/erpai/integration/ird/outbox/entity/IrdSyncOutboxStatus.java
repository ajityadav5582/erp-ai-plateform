package com.erpai.integration.ird.outbox.entity;

/**
 * Lifecycle status of an IRD CBMS sync outbox entry.
 */
public enum IrdSyncOutboxStatus {
    PENDING,
    SYNCED,
    SIMULATED,
    FAILED,
    SKIPPED,
    DEAD_LETTER
}
