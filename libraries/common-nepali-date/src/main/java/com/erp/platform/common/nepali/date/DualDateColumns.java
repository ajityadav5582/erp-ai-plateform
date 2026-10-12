package com.erp.platform.common.nepali.date;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Reusable JPA embeddable holding a Gregorian date, its Bikram Sambat projection and the
 * structured BS components, kept consistent automatically.
 *
 * <p><b>Why this exists.</b> Every document in this ERP (purchase order, invoice, journal
 * entry, GRN, employee appointment) needs both an AD and a BS date. Storing them as loose
 * columns invites drift, which is exactly how the corrupt {@code master_bs_calendar} table
 * and the mismatched {@code master_fiscal_years} AD/BS pairs got in. Embedding this type
 * makes divergence structurally impossible.
 *
 * <p><b>Column layout</b> when embedded with prefix {@code issue_}:
 * <pre>
 *   issue_ad              date        canonical, indexed, used for all queries
 *   issue_bs              varchar(10) derived, display and Nepali reporting
 *   issue_bs_year         integer     derived, for "FY 2081" style filters
 *   issue_bs_month        integer     derived
 *   issue_bs_day          integer     derived
 *   issue_bs_provisional  boolean     derived, flags projected calendar years
 * </pre>
 *
 * <p><b>Usage</b> (the AD value is the only one you set):
 * <pre>{@code
 * @Embedded(prefix = "issue_")
 * private DualDateColumns issueDate = DualDateColumns.of(LocalDate.of(2024, 4, 13));
 *
 * @Embedded(prefix = "due_")
 * private DualDateColumns dueDate = DualDateColumns.of(LocalDate.of(2024, 5, 15));
 * }</pre>
 *
 * <p><b>Consistency rules enforced</b>
 * <ul>
 *   <li>{@link #setAdDate(LocalDate)} always recomputes every BS column.</li>
 *   <li>{@link #setBsDate(String)} converts BS to AD and recomputes the structured columns,
 *       so a caller may enter either calendar and the row still agrees.</li>
 *   <li>{@link #sync()} runs on both {@code @PrePersist} and {@code @PreUpdate} and repairs
 *       any column that was mutated directly or loaded from legacy bad data.</li>
 *   <li>{@link #isConsistent()} exposes drift for diagnostics and tests.</li>
 * </ul>
 */
@Embeddable
public class DualDateColumns implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "_ad", nullable = false)
    private LocalDate adDate;

    @Column(name = "_bs", nullable = false, length = 10)
    private String bsDate;

    @Column(name = "_bs_year", nullable = false)
    private Integer bsYear;

    @Column(name = "_bs_month", nullable = false)
    private Integer bsMonth;

    @Column(name = "_bs_day", nullable = false)
    private Integer bsDay;

    @Column(name = "_bs_provisional", nullable = false)
    private Boolean bsProvisional;

    /** JPA and no-arg constructor. */
    protected DualDateColumns() {
    }

    private DualDateColumns(LocalDate adDate) {
        setAdDate(adDate);
    }

    /** Creates a dual date from a canonical Gregorian date. */
    public static DualDateColumns of(LocalDate adDate) {
        return new DualDateColumns(adDate);
    }

    /** Creates a dual date from a Bikram Sambat date in {@code yyyy-MM-dd} form. */
    public static DualDateColumns ofBs(String bsDateText) {
        return new DualDateColumns(NepaliDateConverter.toAd(bsDateText));
    }

    /** Creates a dual date from a Bikram Sambat date. */
    public static DualDateColumns ofBs(NepaliDate bsDate) {
        return new DualDateColumns(NepaliDateConverter.toAd(bsDate));
    }

    /** Canonical Gregorian date. */
    public LocalDate getAdDate() {
        return adDate;
    }

    /** Sets the Gregorian date and recomputes every BS column. */
    public void setAdDate(LocalDate adDate) {
        this.adDate = adDate;
        recomputeBs();
    }

    /** Derived Bikram Sambat date in {@code yyyy-MM-dd} form. */
    public String getBsDate() {
        return bsDate;
    }

    /**
     * Accepts a BS date, converts it to Gregorian and recomputes the structured columns.
     * Useful when a Nepali user types the date directly.
     */
    public void setBsDate(String bsDate) {
        this.adDate = NepaliDateConverter.toAd(bsDate);
        recomputeBs();
    }

    public Integer getBsYear() {
        return bsYear;
    }

    public void setBsYear(Integer bsYear) {
        this.bsYear = bsYear;
    }

    public Integer getBsMonth() {
        return bsMonth;
    }

    public void setBsMonth(Integer bsMonth) {
        this.bsMonth = bsMonth;
    }

    public Integer getBsDay() {
        return bsDay;
    }

    public void setBsDay(Integer bsDay) {
        this.bsDay = bsDay;
    }

    /** {@code true} when the BS year relies on projected, not published, month lengths. */
    public Boolean getBsProvisional() {
        return bsProvisional;
    }

    public void setBsProvisional(Boolean bsProvisional) {
        this.bsProvisional = bsProvisional;
    }

    /** @return the BS date as a {@link NepaliDate}, or {@code null} when AD is null. */
    @Transient
    public NepaliDate bsDateAsObject() {
        return adDate == null ? null : NepaliDateConverter.fromAd(adDate);
    }

    /** @return display metadata for API responses. */
    @Transient
    public NepaliDateDetails details() {
        return adDate == null ? null : NepaliDateConverter.details(adDate);
    }

    /**
     * @return {@code true} when AD is set and every BS column agrees with it.
     */
    @Transient
    public boolean isConsistent() {
        if (adDate == null) {
            return false;
        }
        try {
            NepaliDate bs = NepaliDateConverter.fromAd(adDate);
            return bs.year() == bsYear
                    && bs.month() == bsMonth
                    && bs.day() == bsDay
                    && bs.toString().equals(bsDate)
                    && Boolean.valueOf(BsCalendarData.isProvisionalYear(bs.year())).equals(bsProvisional);
        } catch (NepaliDateException e) {
            return false;
        }
    }

    /** Recomputes the BS columns from AD. Invoked automatically by JPA. */
    private void recomputeBs() {
        if (adDate == null) {
            this.bsDate = null;
            this.bsYear = null;
            this.bsMonth = null;
            this.bsDay = null;
            this.bsProvisional = null;
            return;
        }
        NepaliDate bs = NepaliDateConverter.fromAd(adDate);
        this.bsDate = bs.toString();
        this.bsYear = bs.year();
        this.bsMonth = bs.month();
        this.bsDay = bs.day();
        this.bsProvisional = BsCalendarData.isProvisionalYear(bs.year());
    }

    /**
     * Repairs any drift between the AD date and the BS projection. Runs before every
     * insert and update, which also fixes rows that were written before this embeddable
     * existed.
     *
     * @throws NepaliDateException if AD is null or cannot be projected to BS
     */
    @PrePersist
    @PreUpdate
    public void sync() {
        if (adDate == null) {
            throw new NepaliDateException(
                    "AD date is required and must be convertible to BS "
                            + BsCalendarData.START_YEAR + "-" + BsCalendarData.END_YEAR);
        }
        recomputeBs();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DualDateColumns other)) {
            return false;
        }
        return java.util.Objects.equals(adDate, other.adDate)
                && java.util.Objects.equals(bsDate, other.bsDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(adDate, bsDate);
    }

    @Override
    public String toString() {
        return adDate + " / " + bsDate;
    }
}
