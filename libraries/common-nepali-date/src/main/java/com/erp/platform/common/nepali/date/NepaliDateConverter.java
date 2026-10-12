package com.erp.platform.common.nepali.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Converts between the Gregorian (AD) and Bikram Sambat (BS) calendars.
 *
 * <p><b>Design rule for the whole platform: the Gregorian date is canonical.</b>
 * Business logic, range queries, indexing, sorting and audit trails must all operate on
 * {@code LocalDate}. The BS value is a derived, human-facing projection. This class is the
 * single place where that projection is produced, so it can be corrected in one edit if the
 * Nepali calendar committee revises published month lengths.
 *
 * <p>Conversion is O(1): the offset from {@link BsCalendarData#EPOCH_AD} is computed once,
 * then the year is found by walking at most {@code 91} year boundaries, and the month by
 * walking at most 12 month lengths.
 *
 * <p>This class is thread-safe and stateless.
 */
public final class NepaliDateConverter {

    private NepaliDateConverter() {
    }

    /** Earliest AD date convertible, i.e. BS {@code START_YEAR}-01-01. */
    public static final LocalDate MIN_AD_DATE = BsCalendarData.EPOCH_AD;

    /** Latest AD date convertible, i.e. the last day of BS {@code END_YEAR}. */
    public static final LocalDate MAX_AD_DATE = BsCalendarData.EPOCH_AD.plusDays(
            totalDaysInTable() - 1L);

    private static long totalDaysInTable() {
        long total = 0;
        for (int y = BsCalendarData.START_YEAR; y <= BsCalendarData.END_YEAR; y++) {
            total += BsCalendarData.daysInYear(y);
        }
        return total;
    }

    /**
     * Converts a Gregorian date to Bikram Sambat.
     *
     * @param adDate Gregorian date, must be non-null and inside the supported range
     * @return the equivalent {@link NepaliDate}
     * @throws NepaliDateException if {@code adDate} is null or outside the supported range
     */
    public static NepaliDate fromAd(LocalDate adDate) {
        if (adDate == null) {
            throw new NepaliDateException("AD date must not be null");
        }
        if (adDate.isBefore(MIN_AD_DATE) || adDate.isAfter(MAX_AD_DATE)) {
            throw new NepaliDateException("AD date " + adDate + " is outside the convertible range "
                    + MIN_AD_DATE + ".." + MAX_AD_DATE
                    + " (BS " + BsCalendarData.START_YEAR + "-" + BsCalendarData.END_YEAR + ")");
        }

        long offset = ChronoUnit.DAYS.between(MIN_AD_DATE, adDate);

        int year = BsCalendarData.START_YEAR;
        while (year <= BsCalendarData.END_YEAR && offset >= BsCalendarData.daysInYear(year)) {
            offset -= BsCalendarData.daysInYear(year);
            year++;
        }

        int month = 1;
        int[] months = BsCalendarData.MONTH_DAYS[year - BsCalendarData.START_YEAR];
        while (month <= 12 && offset >= months[month - 1]) {
            offset -= months[month - 1];
            month++;
        }

        return new NepaliDate(year, month, (int) offset + 1);
    }

    /**
     * Converts a Bikram Sambat date to Gregorian.
     *
     * @param bsDate BS date, must be non-null
     * @return the equivalent {@link LocalDate}
     * @throws NepaliDateException if {@code bsDate} is null
     */
    public static LocalDate toAd(NepaliDate bsDate) {
        if (bsDate == null) {
            throw new NepaliDateException("BS date must not be null");
        }
        long offset = BsCalendarData.yearStartOffset(bsDate.year());
        int[] months = BsCalendarData.MONTH_DAYS[bsDate.year() - BsCalendarData.START_YEAR];
        for (int m = 1; m < bsDate.month(); m++) {
            offset += months[m - 1];
        }
        offset += bsDate.day() - 1L;
        return MIN_AD_DATE.plusDays(offset);
    }

    /**
     * Converts Gregorian year/month/day to Bikram Sambat.
     *
     * @throws NepaliDateException if the components are null or the result is out of range
     */
    public static NepaliDate fromAd(Integer year, Integer month, Integer day) {
        if (year == null || month == null || day == null) {
            throw new NepaliDateException("AD year, month and day must all be non-null");
        }
        return fromAd(LocalDate.of(year, month, day));
    }

    /**
     * Converts BS year/month/day to Gregorian.
     *
     * @throws NepaliDateException if the components are null or the BS date is invalid
     */
    public static LocalDate toAd(Integer bsYear, Integer bsMonth, Integer bsDay) {
        if (bsYear == null || bsMonth == null || bsDay == null) {
            throw new NepaliDateException("BS year, month and day must all be non-null");
        }
        return toAd(NepaliDate.of(bsYear, bsMonth, bsDay));
    }

    /**
     * Parses BS text ({@code yyyy-MM-dd}) and converts it to Gregorian.
     *
     * @throws NepaliDateException if the text is malformed or the date is invalid
     */
    public static LocalDate toAd(String bsDateText) {
        return toAd(NepaliDate.parse(bsDateText));
    }

    /**
     * Full conversion with display metadata: the input Gregorian date plus its BS
     * equivalent, month and weekday names in English and Nepali, weekend and
     * provisional-year flags. This is what API endpoints should return.
     *
     * @throws NepaliDateException if {@code adDate} is null or out of range
     */
    public static NepaliDateDetails details(LocalDate adDate) {
        NepaliDate bs = fromAd(adDate);
        DayOfWeek dow = adDate.getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        return new NepaliDateDetails(
                adDate,
                bs.year(),
                bs.month(),
                bs.day(),
                bs.toString(),
                toNepaliDigits(bs.toString()),
                BsCalendarData.MONTH_NAMES_EN[bs.month()],
                BsCalendarData.MONTH_NAMES_NP[bs.month()],
                dow,
                BsCalendarData.DAY_NAMES_EN[dow.getValue() - 1],
                BsCalendarData.DAY_NAMES_NP[dow.getValue() - 1],
                weekend,
                BsCalendarData.isProvisionalYear(bs.year()));
    }

    /**
     * Formats a Gregorian date as BS text in {@code yyyy-MM-dd} form.
     *
     * @throws NepaliDateException if {@code adDate} is null or out of range
     */
    public static String format(LocalDate adDate) {
        return fromAd(adDate).toString();
    }

    /**
     * Renders a Gregorian date the way Nepali business documents do, for example
     * {@code "2081 Baisakh 01 (Saturday)"}.
     *
     * @throws NepaliDateException if {@code adDate} is null or out of range
     */
    public static String formatLong(LocalDate adDate) {
        NepaliDate bs = fromAd(adDate);
        DayOfWeek dow = adDate.getDayOfWeek();
        return bs.year() + " " + BsCalendarData.MONTH_NAMES_EN[bs.month()] + " "
                + String.format("%02d", bs.day()) + " (" + BsCalendarData.DAY_NAMES_EN[dow.getValue() - 1] + ")";
    }

    /** Converts ASCII digits in {@code text} to Devanagari digits. */
    public static String toNepaliDigits(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c >= '0' && c <= '9' ? (char) ('०' + (c - '0')) : c);
        }
        return sb.toString();
    }

    /** Converts Devanagari digits in {@code text} back to ASCII digits. */
    public static String toAsciiDigits(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c >= '०' && c <= '९' ? (char) ('0' + (c - '०')) : c);
        }
        return sb.toString();
    }

    /** @return {@code true} if {@code adDate} can be converted to BS. */
    public static boolean isConvertible(LocalDate adDate) {
        return adDate != null && !adDate.isBefore(MIN_AD_DATE) && !adDate.isAfter(MAX_AD_DATE);
    }

    /**
     * Generates every BS date in a BS year, useful for seeding {@code master_bs_calendar}.
     *
     * @param bsYear BS year to expand
     * @return one entry per day of that BS year, in chronological order
     * @throws NepaliDateException if the year is outside the supported range
     */
    public static java.util.List<NepaliDateDetails> expandBsYear(int bsYear) {
        if (!BsCalendarData.isSupportedYear(bsYear)) {
            throw new NepaliDateException("Unsupported BS year: " + bsYear);
        }
        int[] months = BsCalendarData.MONTH_DAYS[bsYear - BsCalendarData.START_YEAR];
        java.util.List<NepaliDateDetails> out = new java.util.ArrayList<>(BsCalendarData.daysInYear(bsYear));
        for (int m = 1; m <= 12; m++) {
            for (int d = 1; d <= months[m - 1]; d++) {
                out.add(details(toAd(NepaliDate.of(bsYear, m, d))));
            }
        }
        return out;
    }
}
