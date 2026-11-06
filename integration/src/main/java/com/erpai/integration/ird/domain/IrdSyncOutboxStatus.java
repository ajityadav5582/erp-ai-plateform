package com.erpai.integration.ird.domain;

/**
 * Lifecycle status of an IRD CBMS sync outbox entry.
 */
public enum IrdSyncOutboxStatus {
    PENDING,
    SYNCED,
    FAILED,
    SKIPPED,
    DEAD_LETTER
}
