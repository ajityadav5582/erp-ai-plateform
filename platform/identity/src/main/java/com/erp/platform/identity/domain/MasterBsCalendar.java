package com.erp.platform.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

/** Global master entity maintaining exact mapping between Nepali Bikram Sambat (BS) and Gregorian (AD) dates. */
@Entity
@Table(
        name = "master_bs_calendar",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_master_bs_cal_ad_date", columnNames = "ad_date"),
                @UniqueConstraint(name = "uk_master_bs_cal_bs_date", columnNames = "bs_date")
        },
        indexes = {
                @Index(name = "idx_master_bs_cal_ad_date", columnList = "ad_date"),
                @Index(name = "idx_master_bs_cal_bs_date", columnList = "bs_date"),
                @Index(name = "idx_master_bs_cal_bs_year_month", columnList = "bs_year, bs_month"),
                @Index(name = "idx_master_bs_cal_fy_id", columnList = "master_fiscal_year_id")
        }
)
@Check(name = "check_bs_calendar_month_day", constraints = "bs_month >= 1 AND bs_month <= 12 AND bs_day >= 1 AND bs_day <= 32 AND day_of_week >= 1 AND day_of_week <= 7")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MasterBsCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ad_date", nullable = false, unique = true)
    private LocalDate adDate;

    @Column(name = "bs_date", nullable = false, length = 10, unique = true)
    private String bsDate;

    @Column(name = "bs_year", nullable = false)
    private Integer bsYear;

    @Column(name = "bs_month", nullable = false)
    private Integer bsMonth;

    @Column(name = "bs_day", nullable = false)
    private Integer bsDay;

    @Column(name = "bs_month_name_np", nullable = false, length = 50)
    private String bsMonthNameNp;

    @Column(name = "bs_month_name_en", nullable = false, length = 50)
    private String bsMonthNameEn;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "day_name_en", nullable = false, length = 20)
    private String dayNameEn;

    @Column(name = "day_name_np", nullable = false, length = 50)
    private String dayNameNp;

    @Column(name = "is_holiday", nullable = false, columnDefinition = "boolean default false")
    private Boolean isHoliday = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_fiscal_year_id")
    private MasterFiscalYear masterFiscalYear;

    @Column(name = "created_at", columnDefinition = "timestamptz default CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamptz default CURRENT_TIMESTAMP")
    private Instant updatedAt;

    public static MasterBsCalendar create(
            LocalDate adDate,
            String bsDate,
            Integer bsYear,
            Integer bsMonth,
            Integer bsDay,
            String bsMonthNameNp,
            String bsMonthNameEn,
            Integer dayOfWeek,
            String dayNameEn,
            String dayNameNp,
            Boolean isHoliday,
            MasterFiscalYear masterFiscalYear) {
        MasterBsCalendar calendar = new MasterBsCalendar();
        calendar.adDate = adDate;
        calendar.bsDate = bsDate;
        calendar.bsYear = bsYear;
        calendar.bsMonth = bsMonth;
        calendar.bsDay = bsDay;
        calendar.bsMonthNameNp = bsMonthNameNp;
        calendar.bsMonthNameEn = bsMonthNameEn;
        calendar.dayOfWeek = dayOfWeek;
        calendar.dayNameEn = dayNameEn;
        calendar.dayNameNp = dayNameNp;
        calendar.isHoliday = isHoliday != null ? isHoliday : false;
        calendar.masterFiscalYear = masterFiscalYear;
        return calendar;
    }

    public void setMasterFiscalYear(MasterFiscalYear masterFiscalYear) {
        this.masterFiscalYear = masterFiscalYear;
    }

    public void setIsHoliday(Boolean isHoliday) {
        this.isHoliday = isHoliday != null ? isHoliday : false;
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
        if (isHoliday == null) {
            isHoliday = false;
        }
    }

    @PreUpdate
    private void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }
}
