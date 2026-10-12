package com.erp.platform.common.nepali.date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests that lock the BS calendar to real-world Bikram Sambat New Year dates.
 *
 * <p>These anchors are the reason the previous hand-written table was wrong: it produced
 * BS 2081-01-02 for 2024-04-13. Every anchor below is independently verifiable.
 */
class NepaliDateConverterTest {

    @ParameterizedTest(name = "BS {0}-01-01 == AD {1}")
    @CsvSource({
            "2070, 2013-04-14",
            "2071, 2014-04-14",
            "2072, 2015-04-14",
            "2073, 2016-04-13",
            "2074, 2017-04-14",
            "2075, 2018-04-14",
            "2076, 2019-04-14",
            "2077, 2020-04-13",
            "2078, 2021-04-14",
            "2079, 2022-04-14",
            "2080, 2023-04-14",
            "2081, 2024-04-13",
            "2082, 2025-04-14",
            "2083, 2026-04-14",
            "2084, 2027-04-14",
            "2086, 2029-04-14",
            "2087, 2030-04-14",
            "2090, 2033-04-14",
    })
    @DisplayName("Baisakh 1 of each BS year maps to the published Gregorian date")
    void bsNewYearMatchesPublishedAnchor(int bsYear, String adDate) {
        LocalDate expected = LocalDate.parse(adDate);
        assertEquals(expected, NepaliDateConverter.toAd(NepaliDate.of(bsYear, 1, 1)));
        assertEquals(NepaliDate.of(bsYear, 1, 1), NepaliDateConverter.fromAd(expected));
    }

    @Test
    @DisplayName("Every day from 2070 to 2090 round-trips AD -> BS -> AD")
    void roundTripAcrossEntirePublishedRange() {
        LocalDate start = NepaliDateConverter.toAd(NepaliDate.of(2070, 1, 1));
        // walk to the very last day of BS 2090
        LocalDate end = NepaliDateConverter.toAd(
                NepaliDate.of(2090, 12, BsCalendarData.daysInMonth(2090, 12)));
        LocalDate cursor = start;
        int checked = 0;
        while (!cursor.isAfter(end)) {
            NepaliDate bs = NepaliDateConverter.fromAd(cursor);
            assertEquals(cursor, NepaliDateConverter.toAd(bs),
                    "round-trip failed for AD " + cursor + " / BS " + bs);
            cursor = cursor.plusDays(1);
            checked++;
        }
        // BS 2070..2090 inclusive is exactly 7670 days. Note the previously
        // shipped master_bs_calendar table had 7683 rows for this span: 13 extra
        // days produced by the fabricated month table.
        assertEquals(7670, checked, "expected the true day count for BS 2070..2090");
    }

    @Test
    @DisplayName("Known Nepali national dates convert correctly")
    void knownNepaliDates() {
        // Democracy Day: Falgun 7, 2080 BS == 2024-02-19
        assertEquals(NepaliDate.of(2080, 11, 7), NepaliDateConverter.fromAd(LocalDate.of(2024, 2, 19)));
        assertEquals(LocalDate.of(2024, 2, 19), NepaliDateConverter.toAd(2080, 11, 7));
        // Constitution Day: Asoj 3, 2072 BS == 2015-09-20
        assertEquals(NepaliDate.of(2072, 6, 3), NepaliDateConverter.fromAd(LocalDate.of(2015, 9, 20)));
        // Republic Day: Jestha 15, 2081 BS == 2024-05-28
        assertEquals(NepaliDate.of(2081, 2, 15), NepaliDateConverter.fromAd(LocalDate.of(2024, 5, 28)));
    }

    @Test
    @DisplayName("Last day of a BS year rolls into the next year's Baisakh 1")
    void lastDayOfYearBoundary() {
        int year = 2081;
        int lastMonthDay = BsCalendarData.daysInMonth(year, 12);
        NepaliDate lastDay = NepaliDate.of(year, 12, lastMonthDay);

        // the final day of BS 2081 is Chaitra 31, which is 2025-04-13
        assertEquals(LocalDate.of(2025, 4, 13), NepaliDateConverter.toAd(lastDay));
        assertEquals(lastDay, NepaliDateConverter.fromAd(NepaliDateConverter.toAd(lastDay)));

        // and the very next Gregorian day is Baisakh 1 of BS 2082
        assertEquals(NepaliDate.of(year + 1, 1, 1),
                NepaliDateConverter.fromAd(NepaliDateConverter.toAd(lastDay).plusDays(1)));
        assertEquals(NepaliDateConverter.toAd(lastDay).plusDays(1),
                NepaliDateConverter.toAd(year + 1, 1, 1));
    }

    @Test
    @DisplayName("BS months have realistic lengths and every year has 365 or 366 days")
    void monthLengthSanity() {
        for (int y = BsCalendarData.START_YEAR; y <= BsCalendarData.END_YEAR; y++) {
            int total = 0;
            for (int m = 1; m <= 12; m++) {
                int days = BsCalendarData.daysInMonth(y, m);
                assertTrue(days >= 29 && days <= 32,
                        "implausible month length " + days + " for BS " + y + "-" + m);
                total += days;
            }
            assertTrue(total == 365 || total == 366,
                    "BS " + y + " has " + total + " days, expected 365 or 366");
        }
    }

    @Test
    @DisplayName("Weekday is derived from the Gregorian date")
    void weekdayMatchesGregorian() {
        LocalDate ad = LocalDate.of(2024, 4, 13); // a Saturday
        assertEquals(DayOfWeek.SATURDAY, ad.getDayOfWeek());
        NepaliDateDetails details = NepaliDateConverter.details(ad);
        assertEquals(DayOfWeek.SATURDAY, details.dayOfWeek());
        assertEquals("Saturday", details.dayNameEn());
        assertEquals("शनिबार", details.dayNameNp());
        assertTrue(details.weekend());
    }

    @Test
    @DisplayName("Nepali weekday index matches java.time DayOfWeek value")
    void weekdayNameIndexAlignment() {
        for (LocalDate d = LocalDate.of(2024, 1, 1); d.getYear() == 2024; d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            NepaliDateDetails details = NepaliDateConverter.details(d);
            assertEquals(BsCalendarData.DAY_NAMES_EN[dow.getValue() - 1], details.dayNameEn());
            assertEquals(BsCalendarData.DAY_NAMES_NP[dow.getValue() - 1], details.dayNameNp());
        }
    }

    @Test
    @DisplayName("Sunday is a weekend, Monday is not")
    void sundayWeekendMondayNot() {
        LocalDate sunday = LocalDate.of(2024, 4, 14);
        LocalDate monday = LocalDate.of(2024, 4, 15);
        assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
        assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());
        assertTrue(NepaliDateConverter.details(sunday).weekend());
        assertFalse(NepaliDateConverter.details(monday).weekend());
    }

    @Test
    @DisplayName("Dates outside the supported range are rejected with a clear message")
    void outOfRangeRejected() {
        NepaliDateException before = assertThrows(NepaliDateException.class,
                () -> NepaliDateConverter.fromAd(NepaliDateConverter.MIN_AD_DATE.minusDays(1)));
        assertTrue(before.getMessage().contains("outside the convertible range"));

        assertThrows(NepaliDateException.class,
                () -> NepaliDateConverter.fromAd(NepaliDateConverter.MAX_AD_DATE.plusDays(1)));

        assertThrows(NepaliDateException.class, () -> NepaliDate.of(BsCalendarData.START_YEAR - 1, 1, 1));
        assertThrows(NepaliDateException.class, () -> NepaliDate.of(BsCalendarData.END_YEAR + 1, 1, 1));
        assertThrows(NepaliDateException.class, () -> NepaliDate.of(2081, 13, 1));
        assertThrows(NepaliDateException.class, () -> NepaliDate.of(2081, 0, 1));
    }

    @Test
    @DisplayName("A BS day beyond the real month length is rejected")
    void dayBeyondMonthLengthRejected() {
        int maxDays = BsCalendarData.daysInMonth(2081, 1);
        assertThrows(NepaliDateException.class, () -> NepaliDate.of(2081, 1, maxDays + 1));
        assertThrows(NepaliDateException.class, () -> NepaliDate.of(2081, 1, 0));
    }

    @Test
    @DisplayName("Null inputs are rejected rather than silently defaulted")
    void nullRejected() {
        assertThrows(NepaliDateException.class, () -> NepaliDateConverter.fromAd((LocalDate) null));
        assertThrows(NepaliDateException.class, () -> NepaliDateConverter.toAd((NepaliDate) null));
        assertThrows(NepaliDateException.class, () -> NepaliDateConverter.fromAd(null, null, null));
        assertThrows(NepaliDateException.class, () -> NepaliDateConverter.toAd(null, null, null));
    }

    @Test
    @DisplayName("BS text parses and formats as yyyy-MM-dd")
    void parseAndFormat() {
        assertEquals(NepaliDate.of(2081, 1, 1), NepaliDate.parse("2081-01-01"));
        assertEquals(NepaliDate.of(2081, 1, 1), NepaliDate.parse("2081/01/01"));
        assertEquals("2081-01-01", NepaliDate.of(2081, 1, 1).toString());
        assertEquals("2081-01-01", NepaliDateConverter.format(LocalDate.of(2024, 4, 13)));
        assertTrue(NepaliDate.isValid("2081-01-01"));
        // Jestha 2081 really has 32 days, so day 30 is valid and day 33 is not
        assertEquals(32, BsCalendarData.daysInMonth(2081, 2));
        assertTrue(NepaliDate.isValid("2081-02-30"));
        assertFalse(NepaliDate.isValid("2081-02-33"));
        // Shrawan 2081 has 32 days, Kartik 2081 has only 30
        assertFalse(NepaliDate.isValid("2081-07-31"));
        assertFalse(NepaliDate.isValid("not-a-date"));
        assertFalse(NepaliDate.isValid(null));
    }

    @Test
    @DisplayName("Malformed BS text produces a helpful error, not a NumberFormatException")
    void malformedTextRejected() {
        assertThrows(NepaliDateException.class, () -> NepaliDate.parse("2081-01"));
        assertThrows(NepaliDateException.class, () -> NepaliDate.parse("20x1-01-01"));
        assertThrows(NepaliDateException.class, () -> NepaliDate.parse(""));
    }

    @Test
    @DisplayName("Devanagari digit conversion round-trips")
    void devanagariDigits() {
        assertEquals("२०८१-०१-०१", NepaliDateConverter.toNepaliDigits("2081-01-01"));
        assertEquals("2081-01-01", NepaliDateConverter.toAsciiDigits("२०८१-०१-०१"));
    }

    @Test
    @DisplayName("Long formatting matches Nepali business document style")
    void longFormat() {
        assertEquals("2081 Baisakh 01 (Saturday)", NepaliDateConverter.formatLong(LocalDate.of(2024, 4, 13)));
    }

    @Test
    @DisplayName("Provisional years are flagged from 2085 onward")
    void provisionalFlag() {
        assertFalse(BsCalendarData.isProvisionalYear(2084));
        assertTrue(BsCalendarData.isProvisionalYear(2085));
        assertFalse(NepaliDateConverter.details(LocalDate.of(2027, 6, 1)).provisional());
    }

    @Test
    @DisplayName("Expanding a BS year yields exactly that many consecutive days")
    void expandBsYearIsContiguous() {
        var days = NepaliDateConverter.expandBsYear(2081);
        assertEquals(BsCalendarData.daysInYear(2081), days.size());
        for (int i = 1; i < days.size(); i++) {
            assertEquals(days.get(i - 1).adDate().plusDays(1), days.get(i).adDate());
        }
    }

    @Test
    @DisplayName("Nepali month and weekday names are exact, not just non-empty")
    void nepaliNamesAreExact() {
        assertArrayEquals(
                new String[]{"", "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Aswin",
                        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"},
                BsCalendarData.MONTH_NAMES_EN);
        assertArrayEquals(
                new String[]{"", "\u092c\u0948\u0936\u093e\u0916", "\u091c\u0947\u0920", "\u0905\u0938\u093e\u0930",
                        "\u0936\u094d\u0930\u093e\u0935\u0923", "\u092d\u093e\u0926\u094d\u0930", "\u0906\u0936\u094d\u0935\u093f\u0928",
                        "\u0915\u093e\u0930\u094d\u0924\u093f\u0915", "\u092e\u0902\u0938\u093f\u0930", "\u092a\u094c\u0937", "\u092e\u093e\u0918",
                        "\u092b\u093e\u0932\u094d\u0917\u0941\u0928", "\u091a\u0948\u0924\u094d\u0930"},
                BsCalendarData.MONTH_NAMES_NP);
        // Regression guard: "mangalbar" was once emitted as ma+uu+anusvara+ga
        // (\u092e\u0909\u0902\u0917...) instead of the correct ma+ng+virama+ga.
        assertArrayEquals(
                new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"},
                BsCalendarData.DAY_NAMES_EN);
        assertArrayEquals(
                new String[]{
                        "\u0938\u094b\u092e\u092c\u093e\u0930",                      // sombar
                        "\u092e\u0919\u094d\u0917\u0932\u092c\u093e\u0930",        // mangalbar
                        "\u092c\u0941\u0927\u092c\u093e\u0930",                      // budhbar
                        "\u092c\u093f\u0939\u0940\u092c\u093e\u0930",                // bihibar
                        "\u0936\u0941\u0915\u094d\u0930\u092c\u093e\u0930",        // shukrabar
                        "\u0936\u0928\u093f\u092c\u093e\u0930",                      // shanibar
                        "\u0906\u0907\u0924\u092c\u093e\u0930"},                     // aaitabar
                BsCalendarData.DAY_NAMES_NP);
    }
}
