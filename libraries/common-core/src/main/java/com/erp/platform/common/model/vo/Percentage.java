package com.erp.platform.common.model.vo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a percentage.
 *
 * <p>Ensures type safety for percentage values and prevents
 * invalid percentages (outside 0-100 range).
 *
 * @since 1.0.0
 */
public record Percentage(BigDecimal value) {

    private static final BigDecimal MIN_VALUE = BigDecimal.ZERO;
    private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(100);
    private static final int DEFAULT_SCALE = 2;

    /**
     * Creates a new Percentage instance.
     *
     * @param value the percentage value (0-100)
     * @throws IllegalArgumentException if value is null or outside 0-100 range
     */
    public Percentage {
        Objects.requireNonNull(value, "Percentage value cannot be null");

        BigDecimal normalized = value.setScale(DEFAULT_SCALE);
        if (normalized.compareTo(MIN_VALUE) < 0 || normalized.compareTo(MAX_VALUE) > 0) {
            throw new IllegalArgumentException(
                "Percentage must be between 0 and 100: " + value);
        }
    }

    /**
     * Creates a Percentage from a double value.
     *
     * @param value the percentage value
     * @return a new Percentage
     */
    public static Percentage of(double value) {
        return new Percentage(BigDecimal.valueOf(value));
    }

    /**
     * Creates a Percentage from a string value.
     *
     * @param value the percentage string
     * @return a new Percentage
     */
    public static Percentage of(String value) {
        return new Percentage(new BigDecimal(value));
    }

    /**
     * Returns the percentage as a decimal (e.g., 50% -> 0.5).
     *
     * @return the decimal value
     */
    public BigDecimal asDecimal() {
        return value.divide(BigDecimal.valueOf(100), DEFAULT_SCALE, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Applies this percentage to an amount.
     *
     * @param amount the amount to apply to
     * @return the calculated value
     */
    public BigDecimal applyTo(BigDecimal amount) {
        return amount.multiply(asDecimal());
    }

    @Override
    public String toString() {
        return value + "%";
    }
}
