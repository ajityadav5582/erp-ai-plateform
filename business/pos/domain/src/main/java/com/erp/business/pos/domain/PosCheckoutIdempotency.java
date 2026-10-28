package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "pos_checkout_idempotency",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pos_checkout_idempotency_scope_key",
                columnNames = {"tenant_id", "company_id", "user_id", "idempotency_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PosCheckoutIdempotency extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @Column(name = "idempotency_key", nullable = false, length = 255, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String requestHash;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "IN_PROGRESS";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void complete(Long transactionId) {
        this.transactionId = transactionId;
        this.status = "COMPLETED";
    }
}
