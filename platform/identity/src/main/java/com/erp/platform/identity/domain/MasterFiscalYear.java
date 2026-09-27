package com.erp.platform.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.time.LocalDate;

/** Global master record describing one financial year in BS and AD calendars. */
@Entity
@Table(
        name = "master_fiscal_years",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_master_fy_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_master_fy_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_master_fy_display_order", columnNames = "display_order")
        },
        indexes = {
                @Index(name = "idx_master_fy_code", columnList = "code"),
                @Index(name = "idx_master_fy_ad_range", columnList = "start_date_ad, end_date_ad")
        }
)
@Check(name = "check_master_ad_dates", constraints = "start_date_ad < end_date_ad")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MasterFiscalYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "smallint")
    private Short id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "start_date_bs", nullable = false, length = 10)
    private String startDateBs;

    @Column(name = "end_date_bs", nullable = false, length = 10)
    private String endDateBs;

    @Column(name = "start_date_ad", nullable = false)
    private LocalDate startDateAd;

    @Column(name = "end_date_ad", nullable = false)
    private LocalDate endDateAd;

    @Column(name = "display_order", nullable = false, columnDefinition = "smallint")
    private Short displayOrder;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @Column(name = "created_at", columnDefinition = "timestamptz default CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamptz default CURRENT_TIMESTAMP")
    private Instant updatedAt;

    public static MasterFiscalYear create(
            String name,
            String code,
            String startDateBs,
            String endDateBs,
            LocalDate startDateAd,
            LocalDate endDateAd,
            Short displayOrder) {
        MasterFiscalYear fiscalYear = new MasterFiscalYear();
        fiscalYear.name = name;
        fiscalYear.code = code;
        fiscalYear.startDateBs = startDateBs;
        fiscalYear.endDateBs = endDateBs;
        fiscalYear.startDateAd = startDateAd;
        fiscalYear.endDateAd = endDateAd;
        fiscalYear.displayOrder = displayOrder;
        fiscalYear.isActive = true;
        return fiscalYear;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    @PrePersist
    private void setCreationTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    private void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }
}
