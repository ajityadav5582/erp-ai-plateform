package com.erp.platform.common.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for date and time operations.
 *
 * <p>All timestamps in the system should be stored in UTC.
 * This utility provides conversion and formatting helpers.
 *
 * @since 1.0.0
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // Utility class
    }

    public static final String ISO_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String ISO_DATE = "yyyy-MM-dd";
    public static final String ISO_TIME = "HH:mm:ss";
    public static final String DISPLAY_DATE_TIME = "dd MMM yyyy, hh:mm a";
    public static final String DISPLAY_DATE = "dd MMM yyyy";

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneOffset UTC_OFFSET = ZoneOffset.UTC;

    /**
     * Returns the current instant in UTC.
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Converts LocalDateTime to Instant (UTC).
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(UTC).toInstant();
    }

    /**
     * Converts LocalDate to Instant at start of day (UTC).
     */
    public static Instant toInstant(LocalDate localDate) {
        return localDate.atStartOfDay(UTC).toInstant();
    }

    /**
     * Converts Instant to LocalDateTime in UTC.
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, UTC);
    }

    /**
     * Converts Instant to LocalDate in UTC.
     */
    public static LocalDate toLocalDate(Instant instant) {
        return toLocalDateTime(instant).toLocalDate();
    }

    /**
     * Formats an Instant to ISO date-time string.
     */
    public static String formatDateTime(Instant instant) {
        return format(instant, ISO_DATE_TIME);
    }

    /**
     * Formats an Instant to ISO date string.
     */
    public static String formatDate(Instant instant) {
        return format(instant, ISO_DATE);
    }

    /**
     * Formats an Instant using the specified pattern.
     */
    public static String format(Instant instant, String pattern) {
        if (instant == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(toLocalDateTime(instant));
    }

    /**
     * Parses an ISO date-time string to Instant.
     */
    public static Instant parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
            return toInstant(localDateTime);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date-time format: " + dateTime, e);
        }
    }

    /**
     * Parses an ISO date string to Instant.
     */
    public static Instant parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            LocalDate localDate = LocalDate.parse(date, formatter);
            return toInstant(localDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + date, e);
        }
    }

    /**
     * Checks if an instant is today (UTC).
     */
    public static boolean isToday(Instant instant) {
        LocalDate instantDate = toLocalDate(instant);
        LocalDate today = toLocalDate(now());
        return instantDate.equals(today);
    }

    /**
     * Returns the start of day for the given instant (UTC).
     */
    public static Instant startOfDay(Instant instant) {
        return toInstant(toLocalDate(instant));
    }

    /**
     * Returns the end of day for the given instant (UTC).
     */
    public static Instant endOfDay(Instant instant) {
        LocalDate localDate = toLocalDate(instant);
        return localDate.plusDays(1).atStartOfDay(UTC).toInstant().minusNanos(1);
    }

    /**
     * Returns the start of month for the given instant (UTC).
     */
    public static Instant startOfMonth(Instant instant) {
        LocalDate localDate = toLocalDate(instant);
        return toInstant(localDate.withDayOfMonth(1));
    }

    /**
     * Returns the end of month for the given instant (UTC).
     */
    public static Instant endOfMonth(Instant instant) {
        LocalDate localDate = toLocalDate(instant);
        return localDate.withDayOfMonth(localDate.lengthOfMonth()).plusDays(1).atStartOfDay(UTC).toInstant().minusNanos(1);
    }

    /**
     * Adds days to an instant.
     */
    public static Instant plusDays(Instant instant, long days) {
        return instant.plusSeconds(days * 24 * 60 * 60);
    }

    /**
     * Adds hours to an instant.
     */
    public static Instant plusHours(Instant instant, long hours) {
        return instant.plusSeconds(hours * 60 * 60);
    }

    /**
     * Adds minutes to an instant.
     */
    public static Instant plusMinutes(Instant instant, long minutes) {
        return instant.plusSeconds(minutes * 60);
    }

    /**
     * Calculates the duration between two instants in seconds.
     */
    public static long durationSeconds(Instant start, Instant end) {
        return end.getEpochSecond() - start.getEpochSecond();
    }

    /**
     * Calculates the duration between two instants in milliseconds.
     */
    public static long durationMillis(Instant start, Instant end) {
        return end.toEpochMilli() - start.toEpochMilli();
    }
}
