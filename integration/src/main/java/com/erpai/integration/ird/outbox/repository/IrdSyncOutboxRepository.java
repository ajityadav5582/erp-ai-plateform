package com.erpai.integration.ird.outbox.repository;

import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
public interface IrdSyncOutboxRepository extends JpaRepository<IrdSyncOutboxEntity, UUID> {

    @Query(value = """
            SELECT * FROM ird_sync_outbox
            WHERE sync_status IN ('PENDING', 'FAILED')
              AND next_retry_at <= :now
              AND retry_count < max_retries
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<IrdSyncOutboxEntity> fetchPendingRecordsForUpdate(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE IrdSyncOutboxEntity e
            SET e.syncStatus = com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus.SYNCED,
                e.lastResponseCode = :responseCode,
                e.lastAttemptedAt = :attemptedAt,
                e.updatedAt = :attemptedAt
            WHERE e.id = :id
            """)
    void markSynced(@Param("id") UUID id, @Param("responseCode") int responseCode, @Param("attemptedAt") Instant attemptedAt);

    @Modifying
    @Query("UPDATE IrdSyncOutboxEntity e SET e.syncStatus = com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus.SIMULATED, e.lastResponseCode = :responseCode, e.lastAttemptedAt = :attemptedAt, e.updatedAt = :attemptedAt WHERE e.id = :id")
    void markSimulated(@Param("id") UUID id, @Param("responseCode") int responseCode, @Param("attemptedAt") Instant attemptedAt);

    @Modifying
    @Query("""
            UPDATE IrdSyncOutboxEntity e
            SET e.syncStatus = com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus.FAILED,
                e.lastResponseCode = :responseCode,
                e.lastErrorMessage = :errorMessage,
                e.retryCount = :retryCount,
                e.nextRetryAt = :nextRetryAt,
                e.lastAttemptedAt = CURRENT_TIMESTAMP,
                e.updatedAt = CURRENT_TIMESTAMP
            WHERE e.id = :id
            """)
    void markFailed(@Param("id") UUID id,
                    @Param("responseCode") int responseCode,
                    @Param("errorMessage") String errorMessage,
                    @Param("retryCount") int retryCount,
                    @Param("nextRetryAt") Instant nextRetryAt);

    @Modifying
    @Query("""
            UPDATE IrdSyncOutboxEntity e
            SET e.syncStatus = com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus.DEAD_LETTER,
                e.lastResponseCode = :responseCode,
                e.lastErrorMessage = :errorMessage,
                e.lastAttemptedAt = CURRENT_TIMESTAMP,
                e.updatedAt = CURRENT_TIMESTAMP
            WHERE e.id = :id
            """)
    void markDeadLetter(@Param("id") UUID id, @Param("responseCode") int responseCode, @Param("errorMessage") String errorMessage);

    Optional<IrdSyncOutboxEntity> findByCompanyIdAndSourceTransactionId(Long companyId, Long sourceTransactionId);

    long countByCompanyIdAndSyncStatus(Long companyId, com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus status);

    List<IrdSyncOutboxEntity> findByCompanyIdAndSyncStatusIn(Long companyId, List<com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus> statuses);
}
