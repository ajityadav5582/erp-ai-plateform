package com.erpai.integration.ird.monitor;

import com.erpai.integration.ird.outbox.entity.IrdInvoiceType;
import com.erpai.integration.ird.outbox.entity.IrdSourceModule;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus;

import java.time.Instant;
import java.util.UUID;

/** Operator-safe view of a failed outbox record (excludes its serialized payload). */
public record IrdSyncFailureDto(
        UUID id,
        IrdSourceModule sourceModule,
        Long sourceTransactionId,
        String invoiceNumber,
        IrdInvoiceType invoiceType,
        IrdSyncOutboxStatus syncStatus,
        int retryCount,
        int maxRetries,
        Instant nextRetryAt,
        Instant lastAttemptedAt,
        Integer lastResponseCode,
        String lastErrorMessage
) {
    public static IrdSyncFailureDto from(IrdSyncOutboxEntity entity) {
        return new IrdSyncFailureDto(
                entity.getId(), entity.getSourceModule(), entity.getSourceTransactionId(),
                entity.getInvoiceNumber(), entity.getInvoiceType(), entity.getSyncStatus(),
                entity.getRetryCount(), entity.getMaxRetries(), entity.getNextRetryAt(),
                entity.getLastAttemptedAt(), entity.getLastResponseCode(), entity.getLastErrorMessage());
    }
}
