package com.erp.platform.common.nepali.date;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * An immutable Bikram Sambat (BS / Nepali) calendar date.
 *
 * <p>Deliberately framework-free: no JPA, no Jackson, no Spring, so it can be used
 * from domain modules, application services, batch jobs and API layers alike.
 *
 * <p>Instances are always valid: the canonical constructor rejects out-of-range years,
 * months and days using {@link BsCalendarData#daysInMonth(int, int)}.
 *
 * @param year  BS year, within {@link BsCalendarData#START_YEAR}..{@link BsCalendarData#END_YEAR}
 * @param month BS month, 1..12
 * @param day   BS day, 1..days in that month
 */
public record NepaliDate(int year, int month, int day) implements Comparable<NepaliDate>, Serializable {

    private static final long serialVersionUID = 1L;

    /** Validates the components and returns the date. */
    public NepaliDate {
        if (!BsCalendarData.isSupportedYear(year)) {
            throw new NepaliDateException("Unsupported BS year: " + year
                    + " (supported: " + BsCalendarData.START_YEAR + ".." + BsCalendarData.END_YEAR + ")");
        }
        if (month < 1 || month > 12) {
            throw new NepaliDateException("BS month must be between 1 and 12 but was: " + month);
        }
        int maxDay = BsCalendarData.daysInMonth(year, month);
        if (day < 1 || day > maxDay) {
            throw new NepaliDateException(
                    "BS day must be between 1 and " + maxDay + " for "
                            + year + "-" + month + " but was: " + day);
        }
    }

    /**
     * Convenience factory. Equivalent to {@code new NepaliDate(year, month, day)} but
     * reads better in mapping/builder code.
     */
    public static NepaliDate of(int year, int month, int day) {
        return new NepaliDate(year, month, day);
    }

    /**
     * Parses a BS date in {@code yyyy-MM-dd} form, for example {@code "2081-01-01"}.
     *
     * @throws NepaliDateException if the text is null, blank or malformed
     */
    public static NepaliDate parse(String text) {
        if (text == null || text.isBlank()) {
            throw new NepaliDateException("BS date text must not be null or blank");
        }
        String trimmed = text.trim();
        String[] parts = trimmed.split("[-/]");
        if (parts.length != 3) {
            throw new NepaliDateException(
                    "Malformed BS date '" + text + "': expected format yyyy-MM-dd");
        }
        try {
            return new NepaliDate(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()));
        } catch (NumberFormatException e) {
            throw new NepaliDateException("Malformed BS date '" + text + "': non-numeric component", e);
        }
    }

    /** @return {@code true} if {@code text} parses as a valid BS date. */
    public static boolean isValid(String text) {
        try {
            parse(text);
            return true;
        } catch (NepaliDateException e) {
            return false;
        }
    }

    /** @return the Gregorian equivalent of this BS date. */
    public LocalDate toAdDate() {
        return NepaliDateConverter.toAd(this);
    }

    /** @return {@code true} if this date falls on Saturday or Sunday. */
    public boolean isWeekend() {
        return toAdDate().getDayOfWeek() == BsCalendarData.WEEKEND_DAY
                || toAdDate().getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
    }

    /** @return {@code true} if the underlying year uses only provisional month lengths. */
    public boolean isProvisionalYear() {
        return BsCalendarData.isProvisionalYear(year);
    }

    /** @return days in this date's BS month. */
    public int lengthOfMonth() {
        return BsCalendarData.daysInMonth(year, month);
    }

    /** @return English month name, for example {@code "Baisakh"}. */
    public String monthNameEn() {
        return BsCalendarData.MONTH_NAMES_EN[month];
    }

    /** @return Nepali month name, for example {@code "बैशाख"}. */
    public String monthNameNp() {
        return BsCalendarData.MONTH_NAMES_NP[month];
    }

    /** Chronological ordering by BS year, then month, then day. */
    @Override
    public int compareTo(NepaliDate other) {
        int cmp = Integer.compare(year, other.year);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(month, other.month);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(day, other.day);
    }

    /**
     * ISO-8601 style {@code yyyy-MM-dd} text using BS components.
     * This is the canonical storage/transport format for a BS date.
     */
    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d", year, month, day);
    }
}
