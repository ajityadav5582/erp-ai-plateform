package com.erp.business.procurement.shared.domain;

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
 * A monotonically increasing counter backing human readable document numbers.
 *
 * <p><strong>Why a table of counters rather than a database sequence.</strong> A
 * native sequence is global to the database, so it cannot be partitioned per
 * company or reset per fiscal year. Document numbers in this platform must be
 * scoped to {@code (tenant, company, document type, period)} and must reset for
 * a new fiscal year, which a sequence cannot express.
 *
 * <p><strong>Why this is not a tenant-scoped entity.</strong> It carries tenant
 * and company columns, but it deliberately does not extend
 * {@link TenantScopedEntity}. That base class brings optimistic locking and a
 * {@code @PrePersist} scope check, neither of which fits here: a sequence row is
 * a counter, not a business document, and its row version would conflict with
 * the lock the repository takes on the counter itself.
 *
 * <p><strong>Concurrency.</strong> Allocation must be performed with a
 * pessimistic write lock, or an atomic upsert, inside the same transaction as
 * the document insert. Reading the value, incrementing it in Java and writing
 * it back is a lost-update race that produces duplicate document numbers — the
 * single most damaging defect a numbering scheme can have, because duplicates
 * make an audit trail ambiguous.
 *
 * @since 1.0.0
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

    /** Owning tenant. */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /** Owning company. */
    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    /** Logical document family, for example {@code PURCHASE_REQUISITION}. */
    @Column(name = "document_type", nullable = false, length = 40, updatable = false)
    private String documentType;

    /**
     * Reset period, typically the fiscal year label such as {@code 2081/82}.
     *
     * <p>Part of the unique key so that a new fiscal year starts a fresh counter
     * while the previous year's numbers remain unique.
     */
    @Column(name = "period_key", nullable = false, length = 20, updatable = false)
    private String periodKey;

    /** Number of documents issued in this scope and period. */
    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Long currentValue = 0L;

    @Column(name = "last_allocated_at")
    private Instant lastAllocatedAt;

    /**
     * Creates a counter at zero for a scope and period.
     *
     * @param tenantId     the owning tenant
     * @param companyId    the owning company
     * @param documentType the document family
     * @param periodKey    the reset period
     * @return a new unsaved counter
     */
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
     * Creates a counter that resumes above a number already in use.
     *
     * <p>Needed whenever the period key changes - switching from a calendar-year
     * key to a fiscal-year key creates a partition that has no counter, and a
     * counter starting at zero would reissue PR-000001, which the unique
     * constraint on {@code (tenant_id, company_id, pr_number)} then rejects. The
     * caller seeds from the highest number this scope has ever issued, so the new
     * partition continues the series rather than restarting it.
     *
     * @param tenantId     the owning tenant
     * @param companyId    the owning company
     * @param documentType the document family
     * @param periodKey    the reset period
     * @param resumeAfter  the highest number already issued in this scope
     * @return a new unsaved counter positioned above {@code resumeAfter}
     */
    public static DocumentSequence startAt(
            Long tenantId, Long companyId, String documentType, String periodKey,
            long resumeAfter) {

        if (resumeAfter < 0) {
            throw new IllegalArgumentException("Cannot resume a document counter below zero");
        }
        return DocumentSequence.builder()
                .tenantId(tenantId)
                .companyId(companyId)
                .documentType(documentType)
                .periodKey(periodKey)
                .currentValue(resumeAfter)
                .build();
    }

    /**
     * Increments the counter and records when the allocation happened.
     *
     * <p>Mutating in Java is only safe under a pessimistic write lock held by
     * the repository for the duration of the transaction. Without that lock two
     * concurrent allocations would read the same value and issue the same
     * number.
     *
     * @return the newly allocated number
     */
    public Long allocate() {
        this.currentValue = this.currentValue + 1;
        this.lastAllocatedAt = Instant.now();
        return this.currentValue;
    }

    /**
     * Formats an allocated value as a document number.
     *
     * @param prefix the document prefix, for example {@code PR-}
     * @param width  the minimum zero-padded width
     * @return the formatted number, for example {@code PR-000042}
     */
    public String format(long allocatedValue, String prefix, int width) {
        return prefix + String.format("%0" + Math.max(width, 1) + "d", allocatedValue);
    }
}
