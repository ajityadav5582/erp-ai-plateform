package com.erp.platform.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/** Fiscal year configured for an individual company. */
@Entity
@Table(name = "company_fiscal_years",
        uniqueConstraints = @UniqueConstraint(name = "uk_company_fiscal_year_code", columnNames = {"company_id", "code"}),
        indexes = @Index(name = "idx_company_fiscal_years_company", columnList = "company_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyFiscalYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "master_fiscal_year_id")
    private Short masterFiscalYearId;

    @Column(name = "start_date_bs", length = 10)
    private String startDateBs;

    @Column(name = "end_date_bs", length = 10)
    private String endDateBs;

    @Column(name = "start_date_ad")
    private LocalDate startDateAd;

    @Column(name = "end_date_ad")
    private LocalDate endDateAd;

    // Calendar-specific copies retained for compatibility with databases that
    // already have the original company fiscal year columns.
    @Column(name = "start_date", nullable = false, length = 10)
    private String startDate;

    @Column(name = "end_date", nullable = false, length = 10)
    private String endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_type", nullable = false, length = 20)
    private FiscalYearType calendarType;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static CompanyFiscalYear create(Long tenantId, Long companyId, Short masterFiscalYearId,
                                           String name, String code, String startDateBs, String endDateBs,
                                           LocalDate startDateAd, LocalDate endDateAd,
                                           FiscalYearType calendarType) {
        CompanyFiscalYear year = new CompanyFiscalYear();
        year.tenantId = tenantId;
        year.companyId = companyId;
        year.masterFiscalYearId = masterFiscalYearId;
        year.name = name.trim();
        year.code = code.trim();
        year.startDateBs = startDateBs.trim();
        year.endDateBs = endDateBs.trim();
        year.startDateAd = startDateAd;
        year.endDateAd = endDateAd;
        year.startDate = calendarType == FiscalYearType.NEPALI_BS ? startDateBs.trim() : startDateAd.toString();
        year.endDate = calendarType == FiscalYearType.NEPALI_BS ? endDateBs.trim() : endDateAd.toString();
        year.calendarType = calendarType;
        return year;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
