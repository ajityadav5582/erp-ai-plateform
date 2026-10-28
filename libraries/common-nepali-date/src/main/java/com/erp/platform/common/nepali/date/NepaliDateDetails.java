package com.erp.platform.common.nepali.date;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A fully resolved date: the Gregorian date, its Bikram Sambat equivalent, and the
 * display metadata the UI needs (month and weekday names in both languages, weekend flag).
 *
 * <p>This is what REST controllers should return so the frontend never has to run its own
 * calendar arithmetic.
 *
 * @param adDate       canonical Gregorian date, never null
 * @param bsYear       BS year
 * @param bsMonth      BS month, 1..12
 * @param bsDay        BS day
 * @param bsDate       BS date as {@code yyyy-MM-dd}
 * @param bsDateNp     BS date in Devanagari digits, for example {@code "२०८१-०१-०१"}
 * @param monthNameEn  English month name, for example {@code "Baisakh"}
 * @param monthNameNp  Nepali month name, for example {@code "बैशाख"}
 * @param dayOfWeek    Java {@link DayOfWeek} derived from {@code adDate}
 * @param dayNameEn    English weekday name, for example {@code "Saturday"}
 * @param dayNameNp    Nepali weekday name, for example {@code "शनिबार"}
 * @param weekend      {@code true} for Saturday or Sunday
 * @param provisional  {@code true} when the BS year relies on projected month lengths
 */
public record NepaliDateDetails(
        LocalDate adDate,
        int bsYear,
        int bsMonth,
        int bsDay,
        String bsDate,
        String bsDateNp,
        String monthNameEn,
        String monthNameNp,
        DayOfWeek dayOfWeek,
        String dayNameEn,
        String dayNameNp,
        boolean weekend,
        boolean provisional) implements Serializable {

    private static final long serialVersionUID = 1L;

    /** @return the BS date as a {@link NepaliDate} value object. */
    public NepaliDate bsDateAsObject() {
        return NepaliDate.of(bsYear, bsMonth, bsDay);
    }
}
