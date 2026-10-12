package com.erp.platform.identity.domain;

import java.time.LocalDate;

/**
 * @deprecated Moved to {@code libraries/common-nepali-date}.
 *
 * <p><b>Why this class was removed.</b> The original implementation held its own
 * hand-written copy of the Bikram Sambat month-length table. That table was fabricated:
 * it duplicated month 12 as 31 days for every year and drifted from the real calendar by
 * up to 7 days. It mapped 2024-04-13 to BS 2081-01-02 instead of 2081-01-01, and it had
 * already propagated into all 7683 rows of the {@code master_bs_calendar} table.
 *
 * <p>Calendar arithmetic now lives in one place:
 * <ul>
 *   <li>{@code com.erp.platform.common.nepali.date.BsCalendarData} - month lengths</li>
 *   <li>{@code com.erp.platform.common.nepali.date.NepaliDateConverter} - conversion</li>
 * </ul>
 *
 * <p>This shim only forwards, so existing callers keep compiling while they migrate.
 *
 * @see com.erp.platform.common.nepali.date.NepaliDateConverter
 */
@Deprecated(since = "1.0", forRemoval = true)
public final class BsCalendarData {

    private BsCalendarData() {
    }

    /** @return the BS date in {@code yyyy-MM-dd} form. */
    public static String convertAdToBsString(LocalDate adDate) {
        return com.erp.platform.common.nepali.date.NepaliDateConverter.format(adDate);
    }

    /** @return the Gregorian date matching the BS year/month/day. */
    public static LocalDate convertBsToAd(int year, int month, int day) {
        return com.erp.platform.common.nepali.date.NepaliDateConverter.toAd(year, month, day);
    }
}
