package com.erpai.integration.ird.monitor;

import java.time.Instant;

public record SyncStatusDto(
        long syncedCount,
        long simulatedCount,
        long pendingCount,
        long failedCount,
        long deadLetterCount,
        Instant lastSyncedAt
) {
}
