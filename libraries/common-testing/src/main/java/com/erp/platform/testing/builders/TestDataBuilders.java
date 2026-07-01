package com.erp.platform.testing.builders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test data builders for the ERP AI Platform.
 *
 * <p>This class provides builder methods for creating
 * test data objects.
 *
 * @since 1.0.0
 */
public final class TestDataBuilders {

    private TestDataBuilders() {
        // Utility class
    }

    /**
     * Creates a test UUID.
     *
     * @return a test UUID
     */
    public static UUID uuid() {
        return UUID.randomUUID();
    }

    /**
     * Creates a fixed test UUID.
     *
     * @return a fixed test UUID
     */
    public static UUID fixedUuid() {
        return UUID.fromString("12345678-1234-1234-1234-123456789012");
    }

    /**
     * Creates a test BigDecimal.
     *
     * @param value the value
     * @return the BigDecimal
     */
    public static BigDecimal amount(double value) {
        return BigDecimal.valueOf(value);
    }

    /**
     * Creates a test LocalDateTime.
     *
     * @return the LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Creates a test LocalDate.
     *
     * @return the LocalDate
     */
    public static LocalDate today() {
        return LocalDate.now();
    }
}
