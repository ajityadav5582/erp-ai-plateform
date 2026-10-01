package com.erp.platform.identity.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Standard Bikram Sambat (BS) calendar lookup table and conversion utilities
 * for Nepal BS calendar mapping between 2000 BS and 2090 BS.
 */
public final class BsCalendarData {

    public static final String[] MONTH_NAMES_EN = {
            "", "Baishakh", "Jestha", "Ashadh", "Shrawan", "Bhadra",
            "Ashwin", "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    };

    public static final String[] MONTH_NAMES_NP = {
            "", "वैशाख", "जेठ", "असार", "साउन", "भदौ",
            "असोज", "कात्तिक", "मंसिर", "पुष", "माघ", "फागुन", "चैत"
    };

    public static final String[] DAY_NAMES_EN = {
            "", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    public static final String[] DAY_NAMES_NP = {
            "", "आइतबार", "सोमबार", "मङ्गलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार"
    };

    /** Days in each month (1..12) for BS years 2000 to 2090. */
    public static final int[][] MONTH_DAYS = {
            // 2000
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2001
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2002
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2003
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2004
            {30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2005
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2006
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2007
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2008
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31},
            // 2009
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2010
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2011
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2012
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30},
            // 2013
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2014
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2015
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2016
            {31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30},
            // 2017
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2018
            {31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2019
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2020
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2021
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2022
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30},
            // 2023
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2024
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2025
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2026
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2027
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2028
            {31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2029
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2030
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2031
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2032
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2033
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2034
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2035
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2036
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2037
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2038
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2039
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2040
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2041
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2042
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2043
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2044
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2045
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2046
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2047
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2048
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2049
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2050
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2051
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2052
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2053
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2054
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2055
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2056
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2057
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2058
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2059
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2060
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2061
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2062
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2063
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2064
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2065
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2066
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2067
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2068
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2069
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2070
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2071
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2072
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2073
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2074
            {31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30},
            // 2075
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2076
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2077
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
            // 2078
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 32},
            // 2079
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 31},
            // 2080
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2081
            {31, 31, 32, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2082
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2083
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 31},
            // 2084
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 31},
            // 2085
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2086
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
            // 2087
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 31},
            // 2088
            {31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 31},
            // 2089
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
            // 2090
            {31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}
    };

    public static final int START_YEAR = 2000;
    public static final int END_YEAR = 2090;
    public static final LocalDate ANCHOR_AD_2083 = LocalDate.of(2026, 4, 14); // BS 2083-01-01
    public static final LocalDate START_AD_DATE = calculateStartAdDate();

    private static LocalDate calculateStartAdDate() {
        long daysBefore2083 = 0;
        for (int y = START_YEAR; y < 2083; y++) {
            for (int d : MONTH_DAYS[y - START_YEAR]) {
                daysBefore2083 += d;
            }
        }
        return ANCHOR_AD_2083.minusDays(daysBefore2083);
    }

    private BsCalendarData() {
    }

    public record BsDateDetails(
            int bsYear,
            int bsMonth,
            int bsDay,
            String bsDateFormatted,
            String monthNameEn,
            String monthNameNp,
            int dayOfWeek,
            String dayNameEn,
            String dayNameNp,
            boolean isHoliday
    ) {
    }

    public static BsDateDetails convertAdToBs(LocalDate adDate) {
        if (adDate == null) {
            throw new IllegalArgumentException("AD date cannot be null");
        }
        long daysDiff = ChronoUnit.DAYS.between(START_AD_DATE, adDate);
        if (daysDiff < 0) {
            throw new IllegalArgumentException("AD date " + adDate + " is before supported BS calendar start date " + START_AD_DATE);
        }

        long remainingDays = daysDiff;
        int currYear = START_YEAR;

        while (currYear <= END_YEAR) {
            int[] months = MONTH_DAYS[currYear - START_YEAR];
            int daysInYear = 0;
            for (int d : months) {
                daysInYear += d;
            }

            if (remainingDays < daysInYear) {
                for (int m = 1; m <= 12; m++) {
                    int daysInMonth = months[m - 1];
                    if (remainingDays < daysInMonth) {
                        int bsDay = (int) remainingDays + 1;
                        String formatted = String.format("%04d-%02d-%02d", currYear, m, bsDay);

                        int dayOfWeek = adDate.getDayOfWeek().getValue() % 7 + 1; // 1=Sun, 7=Sat
                        boolean isHoliday = (dayOfWeek == 7);

                        return new BsDateDetails(
                                currYear,
                                m,
                                bsDay,
                                formatted,
                                MONTH_NAMES_EN[m],
                                MONTH_NAMES_NP[m],
                                dayOfWeek,
                                DAY_NAMES_EN[dayOfWeek],
                                DAY_NAMES_NP[dayOfWeek],
                                isHoliday
                        );
                    }
                    remainingDays -= daysInMonth;
                }
            } else {
                remainingDays -= daysInYear;
                currYear++;
            }
        }

        throw new IllegalArgumentException("AD date " + adDate + " is beyond supported BS calendar year range (" + END_YEAR + ")");
    }

    public static LocalDate convertBsToAd(int bsYear, int bsMonth, int bsDay) {
        if (bsYear < START_YEAR || bsYear > END_YEAR) {
            throw new IllegalArgumentException("BS Year " + bsYear + " is outside supported range [" + START_YEAR + ".." + END_YEAR + "]");
        }
        if (bsMonth < 1 || bsMonth > 12) {
            throw new IllegalArgumentException("BS Month must be between 1 and 12");
        }
        int maxDays = MONTH_DAYS[bsYear - START_YEAR][bsMonth - 1];
        if (bsDay < 1 || bsDay > maxDays) {
            throw new IllegalArgumentException("Invalid BS Day " + bsDay + " for " + bsYear + "-" + String.format("%02d", bsMonth) + ". Max days: " + maxDays);
        }

        long totalDays = 0;
        for (int y = START_YEAR; y < bsYear; y++) {
            for (int d : MONTH_DAYS[y - START_YEAR]) {
                totalDays += d;
            }
        }
        int[] monthDays = MONTH_DAYS[bsYear - START_YEAR];
        for (int m = 1; m < bsMonth; m++) {
            totalDays += monthDays[m - 1];
        }
        totalDays += (bsDay - 1);

        return START_AD_DATE.plusDays(totalDays);
    }
}
