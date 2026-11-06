package com.erp.business.pos.service.sequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fiscal_bill_sequences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fiscal_bill_sequences_scope",
                columnNames = {"tenant_id", "company_id", "fiscal_year_id", "document_type"}))
@Getter
@Setter
public class FiscalBillSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "fiscal_year_id", nullable = false)
    private Long fiscalYearId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "prefix", nullable = false, length = 20)
    private String prefix;

    @Column(name = "current_value", nullable = false)
    private Long currentValue = 0L;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
