package com.erp.platform.shared.domain;

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

    @Column(name = "document_type", nullable = false, length = 40, updatable = false)
    private String documentType;

    @Column(name = "period_key", nullable = false, length = 20, updatable = false)
    private String periodKey;

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Long currentValue = 0L;

    @Column(name = "last_allocated_at")
    private Instant lastAllocatedAt;

    public static DocumentSequence startAt(Long tenantId, Long companyId, String documentType, String periodKey) {
        return DocumentSequence.builder()
                .tenantId(tenantId)
                .companyId(companyId)
                .documentType(documentType)
                .periodKey(periodKey)
                .currentValue(0L)
                .build();
    }

    public static DocumentSequence startAt(Long tenantId, Long companyId, String documentType, String periodKey, long resumeAfter) {
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

    public Long allocate() {
        this.currentValue = this.currentValue + 1;
        this.lastAllocatedAt = Instant.now();
        return this.currentValue;
    }

    public String format(long allocatedValue, String prefix, int width) {
        return prefix + String.format("%0" + Math.max(width, 1) + "d", allocatedValue);
    }
}
