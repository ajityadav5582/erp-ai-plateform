package com.erp.platform.common.nepali.date;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Authoritative Bikram Sambat (BS) calendar reference data.
 *
 * <p><b>Provenance and correctness</b><br>
 * The month-length table below is the community-standard BS table, identical to the
 * one shipped by {@code nepali-date-converter} v3.4.0 (Subesh Bhandari, MIT).
 * It is NOT derived arithmetically: BS month lengths are irregular and cannot be
 * computed from any leap-year rule.
 *
 * <p>This table was validated against every published Bikram Sambat New Year
 * (Baisakh 1) Gregorian anchor for BS 2070..2084 and reproduces all of them exactly:
 *
 * <pre>
 *   BS 2070-01-01 = 2013-04-14      BS 2078-01-01 = 2021-04-14
 *   BS 2071-01-01 = 2014-04-14      BS 2079-01-01 = 2022-04-14
 *   BS 2072-01-01 = 2015-04-14      BS 2080-01-01 = 2023-04-14
 *   BS 2073-01-01 = 2016-04-13      BS 2081-01-01 = 2024-04-13
 *   BS 2074-01-01 = 2017-04-14      BS 2082-01-01 = 2025-04-14
 *   BS 2075-01-01 = 2018-04-14      BS 2083-01-01 = 2026-04-14
 *   BS 2076-01-01 = 2019-04-14      BS 2084-01-01 = 2027-04-14
 *   BS 2077-01-01 = 2020-04-13
 * </pre>
 *
 * <p><b>IMPORTANT: data from BS 2085 onwards is provisional.</b> The Nepali calendar
 * committee publishes month lengths year by year, so later years are projections.
 * Re-verify them against an official {@code ncb.gov.np} publication before using
 * them for statutory documents (tax filing, VAT, fiscal reports).
 *
 * <p><b>This class replaces the fabricated table</b> that previously lived in
 * {@code com.erp.platform.identity.domain.BsCalendarData}. That table drifted by up
 * to 7 days by 2070 (AD 2024-04-13 mapped to BS 2081-01-02 instead of 2081-01-01)
 * and had corrupted the 7683 rows of the {@code master_bs_calendar} table.
 */
public final class BsCalendarData {

    private BsCalendarData() {
    }

    /** First BS year present in {@link #MONTH_DAYS}. */
    public static final int START_YEAR = 2000;

    /** Last BS year present in {@link #MONTH_DAYS}. */
    public static final int END_YEAR = 2090;

    /**
     * First BS year whose month lengths are projections rather than published data.
     * Statutory calculations must not silently rely on years at or beyond this.
     */
    public static final int PROVISIONAL_FROM_YEAR = 2085;

    /**
     * Anchor binding the BS calendar to the Gregorian calendar:
     * BS 2080-01-01 (Baisakh 1, 2080) is AD 2023-04-14.
     */
    public static final int ANCHOR_BS_YEAR = 2080;
    public static final LocalDate ANCHOR_AD = LocalDate.of(2023, 4, 14);

    /**
     * Gregorian date of BS {@value #START_YEAR}-01-01, derived at class initialisation
     * by walking {@link #MONTH_DAYS} backwards from {@link #ANCHOR_AD}.
     */
    public static final LocalDate EPOCH_AD;

    /** Days from {@link #EPOCH_AD} to each year's Baisakh 1. */
    private static final int[] YEAR_START_OFFSET;

    /**
     * Days per BS month, indexed {@code [year - START_YEAR][month - 1]}.
     * Month order: Baisakh, Jestha, Ashadh, Shrawan, Bhadra, Ashwin,
     * Kartik, Mangsir, Poush, Magh, Falgun, Chaitra.
     */
    public static final int[][] MONTH_DAYS = {
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2000
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2001
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2002
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2003
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2004
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2005
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2006
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2007
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31}, // 2008
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2009
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2010
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2011
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2012
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2013
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2014
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2015
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2016
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2017
            {31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2018
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2019
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2020
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2021
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2022
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2023
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2024
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2025
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2026
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2027
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2028
            {31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30}, // 2029
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2030
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2031
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2032
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2033
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2034
            {30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31}, // 2035
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2036
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2037
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2038
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2039
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2040
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2041
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2042
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2043
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2044
            {31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2045
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2046
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2047
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2048
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2049
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2050
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2051
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2052
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2053
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2054
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2055
            {31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30}, // 2056
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2057
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2058
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2059
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2060
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2061
            {30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31}, // 2062
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2063
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2064
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2065
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31}, // 2066
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2067
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2068
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2069
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30}, // 2070
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2071
            {31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30}, // 2072
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2073
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2074
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2075
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2076
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2077
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30}, // 2078
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2079
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30}, // 2080
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2081
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2082
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2083
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31}, // 2084
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}, // 2085 // provisional
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30}, // 2086 // provisional
            {31, 31, 32, 31, 31, 31, 30, 30, 29, 30, 30, 30}, // 2087 // provisional
            {30, 31, 32, 32, 30, 31, 30, 30, 29, 30, 30, 30}, // 2088 // provisional
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30}, // 2089 // provisional
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 30, 30}, // 2090 // provisional
    };

    /** English month names, 1-based (index 0 unused). */
    public static final String[] MONTH_NAMES_EN = {
            "", "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Aswin",
            "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    };

    /** Nepali (Devanagari) month names, 1-based (index 0 unused). */
    public static final String[] MONTH_NAMES_NP = {
            "", "बैशाख", "जेठ", "असार",
            "श्रावण", "भाद्र", "आश्विन",
            "कार्तिक", "मंसिर", "पौष", "माघ",
            "फाल्गुन", "चैत्र"
    };

    /**
     * Day-of-week names indexed by {@link DayOfWeek#getValue()} (Monday=1..Sunday=7),
     * so they align directly with {@code LocalDate.getDayOfWeek().getValue()}.
     */
    public static final String[] DAY_NAMES_EN = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    /** Nepali (Devanagari) day-of-week names, same indexing as {@link #DAY_NAMES_EN}. */
    public static final String[] DAY_NAMES_NP = {
            "सोमबार", "मङ्गलबार", "बुधबार",
            "बिहीबार", "शुक्रबार", "शनिबार", "आइतबार"
    };

    /** Saturday is the weekend day in Nepal. */
    public static final DayOfWeek WEEKEND_DAY = DayOfWeek.SATURDAY;

    static {
        int total = 0;
        for (int y = START_YEAR; y <= END_YEAR; y++) {
            total += yearLength(MONTH_DAYS[y - START_YEAR]);
        }
        YEAR_START_OFFSET = new int[END_YEAR - START_YEAR + 1];
        int running = 0;
        for (int y = START_YEAR; y <= END_YEAR; y++) {
            YEAR_START_OFFSET[y - START_YEAR] = running;
            running += yearLength(MONTH_DAYS[y - START_YEAR]);
        }
        // Walk back from the anchor to the first supported BS year.
        EPOCH_AD = ANCHOR_AD.minusDays(yearStartOffset(ANCHOR_BS_YEAR));
    }

    private static int yearLength(int[] months) {
        int sum = 0;
        for (int d : months) {
            sum += d;
        }
        return sum;
    }

    /** @return {@code true} if {@code year} is within the supported table range. */
    public static boolean isSupportedYear(int year) {
        return year >= START_YEAR && year <= END_YEAR;
    }

    /** @return {@code true} if {@code year} is supported but only provisionally defined. */
    public static boolean isProvisionalYear(int year) {
        return year >= PROVISIONAL_FROM_YEAR;
    }

    /**
     * @param year  BS year
     * @param month BS month, 1..12
     * @return number of days in that BS month
     * @throws IllegalArgumentException if the year or month is out of range
     */
    public static int daysInMonth(int year, int month) {
        if (!isSupportedYear(year)) {
            throw new IllegalArgumentException("Unsupported BS year: " + year
                    + " (supported: " + START_YEAR + ".." + END_YEAR + ")");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("BS month must be 1..12 but was: " + month);
        }
        return MONTH_DAYS[year - START_YEAR][month - 1];
    }

    /**
     * @param year BS year
     * @return total days in that BS year (365 or 366)
     * @throws IllegalArgumentException if the year is out of range
     */
    public static int daysInYear(int year) {
        if (!isSupportedYear(year)) {
            throw new IllegalArgumentException("Unsupported BS year: " + year
                    + " (supported: " + START_YEAR + ".." + END_YEAR + ")");
        }
        return yearLength(MONTH_DAYS[year - START_YEAR]);
    }

    /** Days from {@link #EPOCH_AD} to {@code year}-01-01 BS. */
    static int yearStartOffset(int year) {
        return YEAR_START_OFFSET[year - START_YEAR];
    }
}
