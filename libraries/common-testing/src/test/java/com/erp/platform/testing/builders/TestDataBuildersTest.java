package com.erp.platform.testing.builders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TestDataBuilders}.
 */
class TestDataBuildersTest {

    @Test
    @DisplayName("Should create random UUID")
    void shouldCreateRandomUuid() {
        // When
        UUID uuid = TestDataBuilders.uuid();

        // Then
        assertThat(uuid).isNotNull();
    }

    @Test
    @DisplayName("Should create fixed UUID")
    void shouldCreateFixedUuid() {
        // When
        UUID uuid = TestDataBuilders.fixedUuid();

        // Then
        assertThat(uuid).isEqualTo(UUID.fromString("12345678-1234-1234-1234-123456789012"));
    }

    @Test
    @DisplayName("Should create BigDecimal from double")
    void shouldCreateBigDecimalFromDouble() {
        // When
        BigDecimal amount = TestDataBuilders.amount(100.50);

        // Then
        assertThat(amount).isEqualTo(new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("Should create current LocalDateTime")
    void shouldCreateCurrentLocalDateTime() {
        // When
        LocalDateTime now = TestDataBuilders.now();

        // Then
        assertThat(now).isNotNull();
        assertThat(now).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create current LocalDate")
    void shouldCreateCurrentLocalDate() {
        // When
        LocalDate today = TestDataBuilders.today();

        // Then
        assertThat(today).isNotNull();
        assertThat(today).isEqualTo(LocalDate.now());
    }
}
