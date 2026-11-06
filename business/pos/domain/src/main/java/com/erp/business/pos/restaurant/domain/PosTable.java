package com.erp.business.pos.restaurant.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A physical table in a restaurant. Restaurant-only entity; retail companies
 * never use these. Status drives the table grid UI.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "pos_tables",
        indexes = {
                @Index(name = "idx_pos_tables_tenant_company", columnList = "tenant_id, company_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PosTable extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    public enum Status { FREE, OCCUPIED, ORDERED, RESERVED, BILLING, CLEANING }

    @Size(max = 20)
    @Column(name = "table_number", nullable = false, length = 20)
    private String tableNumber;

    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private int capacity = 4;

    @Size(max = 50)
    @Column(name = "section", length = 50)
    private String section;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.FREE;

    @Column(name = "current_session_id")
    private Long currentSessionId;

    public void occupy(Long sessionId) {
        this.status = Status.OCCUPIED;
        this.currentSessionId = sessionId;
    }

    public void free() {
        this.status = Status.FREE;
        this.currentSessionId = null;
    }

    public void markOrdered() {
        this.status = Status.ORDERED;
    }

    public void markCleaning() {
        this.status = Status.CLEANING;
    }

    public void markBilling() {
        this.status = Status.BILLING;
    }
}
