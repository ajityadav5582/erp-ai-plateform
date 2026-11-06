package com.erp.business.pos.restaurant.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * A kitchen order ticket fired from a table session. Restaurant-only.
 * Status tracks preparation progress; a completed order can be billed.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "pos_kitchen_orders",
        indexes = {
                @Index(name = "idx_pos_kitchen_orders_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_kitchen_orders_table", columnList = "tenant_id, company_id, table_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class KitchenOrder extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    public enum Status { PENDING, PREPARING, READY, SERVED, CANCELLED }

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "pos_transaction_id")
    private Long posTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "course")
    @Builder.Default
    private int course = 1;

    @Column(name = "fired_at", nullable = false)
    @Builder.Default
    private Instant firedAt = Instant.now();

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "waiter", length = 100)
    private String waiter;

    public void markPreparing() { this.status = Status.PREPARING; }
    public void markReady() { this.status = Status.READY; }
    public void markServed() { this.status = Status.SERVED; }
    public void cancel() { this.status = Status.CANCELLED; }
}
