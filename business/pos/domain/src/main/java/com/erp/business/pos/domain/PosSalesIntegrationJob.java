package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "pos_sales_integration_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pos_sales_integration_jobs_transaction",
                columnNames = {"tenant_id", "company_id", "pos_transaction_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PosSalesIntegrationJob extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "pos_transaction_id", nullable = false, updatable = false)
    private Long posTransactionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "PENDING";
        if (nextRetryAt == null) nextRetryAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markInProgress(Instant leaseExpiresAt) {
        this.status = "IN_PROGRESS";
        this.attemptCount++;
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public boolean isLeaseExpired(Instant now) {
        return leaseExpiresAt != null && leaseExpiresAt.isBefore(now);
    }

    public void markPending(String error, Instant nextRetryAt) {
        this.status = "PENDING";
        this.lastError = error;
        this.nextRetryAt = nextRetryAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public void markCompleted() {
        this.status = "COMPLETED";
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.lastError = error;
    }
}
