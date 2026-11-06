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
 * A session occupying a table (open tab). Links to a PosTransaction at billing.
 * Restaurant-only.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "pos_table_sessions",
        indexes = {
                @Index(name = "idx_pos_ts_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_ts_table", columnList = "tenant_id, company_id, table_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TableSession extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    public enum Status { OPEN, BILLED, CLOSED, MERGED }

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "pos_transaction_id")
    private Long posTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(name = "opened_at", nullable = false)
    @Builder.Default
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "merged_into_session_id")
    private Long mergedIntoSessionId;

    public void bill(Long posTransactionId) {
        this.status = Status.BILLED;
        this.posTransactionId = posTransactionId;
    }

    public void close() {
        this.status = Status.CLOSED;
        this.closedAt = Instant.now();
    }

    public void mergeInto(Long targetSessionId) {
        this.status = Status.MERGED;
        this.mergedIntoSessionId = targetSessionId;
        this.closedAt = Instant.now();
    }
}
