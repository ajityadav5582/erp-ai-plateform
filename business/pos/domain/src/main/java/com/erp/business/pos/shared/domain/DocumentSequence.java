package com.erp.business.pos.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Gap-free document-number counter, scoped to {@code (tenant, company, document
 * type, period)}. Backs human-readable numbers such as {@code SES-000042}.
 *
 * <p>Deliberately not a {@code TenantScopedEntity}: it is a counter, not a
 * business document, and its row version would conflict with the pessimistic
 * lock the repository takes on the counter itself.
 */
@Entity
@Table(
        name = "document_sequences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_sequences_scope",
                columnNames = {"tenant_id", "company_id", "document_type", "period_key"}
        ),
        indexes = {
                @Index(name = "idx_document_sequences_scope",
                        columnList = "tenant_id, company_id, document_type")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    /** Document family, for example {@code POS_SESSION}. */
    @Column(name = "document_type", nullable = false, length = 40, updatable = false)
    private String documentType;

    /** Reset period, typically the fiscal-year label such as {@code 2081/82}. */
    @Column(name = "period_key", nullable = false, length = 20, updatable = false)
    private String periodKey;

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Long currentValue = 0L;

    @Column(name = "last_allocated_at")
    private Instant lastAllocatedAt;

    public static DocumentSequence startAt(
            Long tenantId, Long companyId, String documentType, String periodKey) {
        return DocumentSequence.builder()
                .tenantId(tenantId)
                .companyId(companyId)
                .documentType(documentType)
                .periodKey(periodKey)
                .currentValue(0L)
                .build();
    }

    /**
     * Increments the counter. Only safe under the pessimistic write lock the
     * repository holds for the duration of the transaction.
     */
    public Long allocate() {
        this.currentValue = this.currentValue + 1;
        this.lastAllocatedAt = Instant.now();
        return this.currentValue;
    }

    public String format(long allocatedValue, String prefix, int width) {
        return prefix + String.format("%0" + Math.max(width, 1) + "d", allocatedValue);
    }
}
