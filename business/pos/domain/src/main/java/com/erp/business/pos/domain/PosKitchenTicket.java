package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "pos_kitchen_tickets",
        indexes = {
                @Index(name = "idx_pos_kitchen_tickets_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_kitchen_tickets_transaction", columnList = "tenant_id, company_id, pos_transaction_id"),
                @Index(name = "idx_pos_kitchen_tickets_status", columnList = "tenant_id, company_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PosKitchenTicket extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "pos_transaction_id", nullable = false, updatable = false)
    private Long posTransactionId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markPreparing() {
        this.status = "PREPARING";
    }

    public void markReady() {
        this.status = "READY";
    }

    public void markServed() {
        this.status = "SERVED";
    }

    public void cancel() {
        this.status = "CANCELLED";
    }
}
