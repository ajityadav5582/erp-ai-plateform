package com.erp.platform.common.model.vo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a quantity.
 *
 * <p>Ensures type safety for quantity values and prevents
 * negative quantities.
 *
 * @since 1.0.0
 */
public record Quantity(BigDecimal value) {

    private static final BigDecimal MIN_VALUE = BigDecimal.ZERO;
    private static final int DEFAULT_SCALE = 4;

    /**
     * Creates a new Quantity instance.
     *
     * @param value the quantity value
     * @throws IllegalArgumentException if value is null or negative
     */
    public Quantity {
        Objects.requireNonNull(value, "Quantity value cannot be null");

        BigDecimal normalized = value.setScale(DEFAULT_SCALE);
        if (normalized.compareTo(MIN_VALUE) < 0) {
            throw new IllegalArgumentException(
                "Quantity cannot be negative: " + value);
        }
    }

    /**
     * Creates a Quantity from a long value.
     *
     * @param value the quantity value
     * @return a new Quantity
     */
    public static Quantity of(long value) {
        return new Quantity(BigDecimal.valueOf(value));
    }

    /**
     * Creates a Quantity from a double value.
     *
     * @param value the quantity value
     * @return a new Quantity
     */
    public static Quantity of(double value) {
        return new Quantity(BigDecimal.valueOf(value));
    }

    /**
     * Creates a zero quantity.
     *
     * @return a zero Quantity
     */
    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }

    /**
     * Adds another quantity to this one.
     *
     * @param other the quantity to add
     * @return a new Quantity with the sum
     */
    public Quantity add(Quantity other) {
        return new Quantity(this.value.add(other.value));
    }

    /**
     * Subtracts another quantity from this one.
     *
     * @param other the quantity to subtract
     * @return a new Quantity with the difference
     * @throws IllegalArgumentException if result is negative
     */
    public Quantity subtract(Quantity other) {
        BigDecimal result = this.value.subtract(other.value);
        if (result.compareTo(MIN_VALUE) < 0) {
            throw new IllegalArgumentException(
                "Quantity cannot be negative: " + result);
        }
        return new Quantity(result);
    }

    /**
     * Multiplies this quantity by a factor.
     *
     * @param factor the multiplication factor
     * @return a new Quantity with the product
     */
    public Quantity multiply(BigDecimal factor) {
        return new Quantity(this.value.multiply(factor));
    }

    /**
     * Checks if this quantity is greater than another.
     *
     * @param other the other quantity
     * @return true if this quantity is greater
     */
    public boolean isGreaterThan(Quantity other) {
        return this.value.compareTo(other.value) > 0;
    }

    /**
     * Checks if this quantity is less than another.
     *
     * @param other the other quantity
     * @return true if this quantity is less
     */
    public boolean isLessThan(Quantity other) {
        return this.value.compareTo(other.value) < 0;
    }

    /**
     * Checks if this quantity equals another.
     *
     * @param other the other quantity
     * @return true if quantities are equal
     */
    public boolean isEqualTo(Quantity other) {
        return this.value.compareTo(other.value) == 0;
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
