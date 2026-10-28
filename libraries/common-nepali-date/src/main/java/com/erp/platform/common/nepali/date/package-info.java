/**
 * Reusable Bikram Sambat (BS / Nepali) date support for the ERP AI Platform.
 *
 * <p>Entry points:
 * <ul>
 *   <li>{@link com.erp.platform.common.nepali.date.NepaliDateConverter} - AD to BS and
 *       BS to AD conversion, plus display formatting.</li>
 *   <li>{@link com.erp.platform.common.nepali.date.NepaliDate} - immutable BS value object.</li>
 *   <li>{@link com.erp.platform.common.nepali.date.NepaliDateDetails} - a resolved date with
 *       English and Nepali month/weekday names, for API responses.</li>
 *   <li>{@link com.erp.platform.common.nepali.date.DualDateColumns} - JPA embeddable that
 *       stores AD and BS side by side and keeps them in sync automatically.</li>
 *   <li>{@link com.erp.platform.common.nepali.date.BsCalendarData} - reference month lengths.</li>
 * </ul>
 *
 * <p><b>Platform convention: the Gregorian date is always canonical.</b> The BS value is
 * derived for display and Nepali statutory reporting. Never query, sort or index on the BS
 * columns; use {@code *_ad}.
 */
package com.erp.platform.common.nepali.date;
