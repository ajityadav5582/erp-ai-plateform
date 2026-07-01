package com.erp.platform.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DateTimeUtils}.
 *
 * @since 1.0.0
 */
class DateTimeUtilsTest {

    @Test
    void now_shouldReturnCurrentInstant() {
        Instant before = Instant.now();
        Instant now = DateTimeUtils.now();
        Instant after = Instant.now();

        assertThat(now).isBetween(before, after);
    }

    @Test
    void toInstant_shouldConvertLocalDateTimeToUtc() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Instant instant = DateTimeUtils.toInstant(localDateTime);

        assertThat(instant).isEqualTo(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
    }

    @Test
    void toInstant_shouldConvertLocalDateToStartOfDayUtc() {
        LocalDate localDate = LocalDate.of(2024, 1, 15);
        Instant instant = DateTimeUtils.toInstant(localDate);

        assertThat(instant).isEqualTo(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }

    @Test
    void toLocalDateTime_shouldConvertInstantToUtc() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        LocalDateTime localDateTime = DateTimeUtils.toLocalDateTime(instant);

        assertThat(localDateTime).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    @Test
    void toLocalDate_shouldConvertInstantToUtcDate() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        LocalDate localDate = DateTimeUtils.toLocalDate(instant);

        assertThat(localDate).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void formatDateTime_shouldFormatInstant() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String formatted = DateTimeUtils.formatDateTime(instant);

        assertThat(formatted).isEqualTo("2024-01-15T10:30:00.000Z");
    }

    @Test
    void formatDateTime_shouldReturnNullForNull() {
        assertThat(DateTimeUtils.formatDateTime(null)).isNull();
    }

    @Test
    void formatDate_shouldFormatInstant() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String formatted = DateTimeUtils.formatDate(instant);

        assertThat(formatted).isEqualTo("2024-01-15");
    }

    @Test
    void format_shouldFormatWithCustomPattern() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String formatted = DateTimeUtils.format(instant, "yyyy/MM/dd");

        assertThat(formatted).isEqualTo("2024/01/15");
    }

    @Test
    void format_shouldReturnNullForNull() {
        assertThat(DateTimeUtils.format(null, "yyyy-MM-dd")).isNull();
    }

    @Test
    void parseDateTime_shouldParseIsoDateTime() {
        String dateTime = "2024-01-15T10:30:00Z";
        Instant instant = DateTimeUtils.parseDateTime(dateTime);

        assertThat(instant).isEqualTo(Instant.parse(dateTime));
    }

    @Test
    void parseDateTime_shouldReturnNullForNull() {
        assertThat(DateTimeUtils.parseDateTime(null)).isNull();
    }

    @Test
    void parseDateTime_shouldReturnNullForBlank() {
        assertThat(DateTimeUtils.parseDateTime("   ")).isNull();
    }

    @Test
    void parseDateTime_shouldThrowForInvalidFormat() {
        assertThatThrownBy(() -> DateTimeUtils.parseDateTime("not-a-date"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseDate_shouldParseIsoDate() {
        String date = "2024-01-15";
        Instant instant = DateTimeUtils.parseDate(date);

        assertThat(instant).isEqualTo(date.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }

    @Test
    void parseDate_shouldReturnNullForNull() {
        assertThat(DateTimeUtils.parseDate(null)).isNull();
    }

    @Test
    void parseDate_shouldReturnNullForBlank() {
        assertThat(DateTimeUtils.parseDate("   ")).isNull();
    }

    @Test
    void isToday_shouldReturnTrueForToday() {
        Instant today = DateTimeUtils.now();
        assertThat(DateTimeUtils.isToday(today)).isTrue();
    }

    @Test
    void isToday_shouldReturnFalseForYesterday() {
        Instant yesterday = DateTimeUtils.now().minusSeconds(86400);
        assertThat(DateTimeUtils.isToday(yesterday)).isFalse();
    }

    @Test
    void startOfDay_shouldReturnStartOfDay() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Instant startOfDay = DateTimeUtils.startOfDay(instant);

        assertThat(startOfDay).isEqualTo(instant.atZone(ZoneId.of("UTC")).toLocalDate()
            .atStartOfDay(ZoneId.of("UTC")).toInstant());
    }

    @Test
    void endOfDay_shouldReturnEndOfDay() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Instant endOfDay = DateTimeUtils.endOfDay(instant);

        assertThat(endOfDay).isEqualTo(instant.atZone(ZoneId.of("UTC")).toLocalDate()
            .plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().minusNanos(1));
    }

    @Test
    void startOfMonth_shouldReturnStartOfMonth() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Instant startOfMonth = DateTimeUtils.startOfMonth(instant);

        assertThat(startOfMonth).isEqualTo(instant.atZone(ZoneId.of("UTC")).toLocalDate()
            .withDayOfMonth(1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    }

    @Test
    void endOfMonth_shouldReturnEndOfMonth() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Instant endOfMonth = DateTimeUtils.endOfMonth(instant);

        LocalDate endOfMonthDate = instant.atZone(ZoneId.of("UTC")).toLocalDate()
            .withDayOfMonth(instant.atZone(ZoneId.of("UTC")).toLocalDate().lengthOfMonth())
            .plusDays(1).minusNanos(1);
        assertThat(endOfMonth).isEqualTo(endOfMonthDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }

    @Test
    void plusDays_shouldAddDays() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Instant result = DateTimeUtils.plusDays(instant, 5);

        assertThat(result).isEqualTo(instant.plusSeconds(5 * 24 * 60 * 60));
    }

    @Test
    void plusHours_shouldAddHours() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Instant result = DateTimeUtils.plusHours(instant, 3);

        assertThat(result).isEqualTo(instant.plusSeconds(3 * 60 * 60));
    }

    @Test
    void plusMinutes_shouldAddMinutes() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        Instant result = DateTimeUtils.plusMinutes(instant, 30);

        assertThat(result).isEqualTo(instant.plusSeconds(30 * 60));
    }

    @Test
    void durationSeconds_shouldCalculateDuration() {
        Instant start = Instant.parse("2024-01-15T10:00:00Z");
        Instant end = Instant.parse("2024-01-15T10:30:00Z");

        assertThat(DateTimeUtils.durationSeconds(start, end)).isEqualTo(1800);
    }

    @Test
    void durationMillis_shouldCalculateDuration() {
        Instant start = Instant.parse("2024-01-15T10:00:00Z");
        Instant end = Instant.parse("2024-01-15T10:00:01Z");

        assertThat(DateTimeUtils.durationMillis(start, end)).isEqualTo(1000);
    }
}
