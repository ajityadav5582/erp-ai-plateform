package com.erp.platform.common.model.vo;

import com.erp.platform.common.exception.ErpBusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing a monetary amount.
 *
 * <p>This class ensures type safety for monetary values and prevents
 * common errors such as mixing currencies or using floating-point types.
 *
 * <p>All monetary operations are currency-safe and throw exceptions
 * on currency mismatch.
 *
 * @since 1.0.0
 */
public record Money(
    BigDecimal amount,
    Currency currency
) {
    private static final int DEFAULT_SCALE = 4;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    /**
     * Creates a new Money instance.
     *
     * @param amount the monetary amount
     * @param currency the currency
     * @throws IllegalArgumentException if amount is null or negative, or currency is null
     */
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }

        // Normalize scale
        amount = amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Creates a Money instance from a string amount.
     *
     * @param amount the amount as string
     * @param currency the currency
     * @return a new Money instance
     */
    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /**
     * Creates a Money instance from a double amount.
     *
     * @param amount the amount as double
     * @param currency the currency
     * @return a new Money instance
     */
    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Creates a zero Money instance for the given currency.
     *
     * @param currency the currency
     * @return a zero Money instance
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Adds another Money amount to this one.
     *
     * @param other the amount to add
     * @return a new Money instance with the sum
     * @throws ErpBusinessException if currencies don't match
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money amount from this one.
     *
     * @param other the amount to subtract
     * @return a new Money instance with the difference
     * @throws ErpBusinessException if currencies don't match
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new ErpBusinessException("NEGATIVE_AMOUNT",
                "Subtraction resulted in negative amount: " + result);
        }
        return new Money(result, this.currency);
    }

    /**
     * Multiplies this amount by a factor.
     *
     * @param factor the multiplication factor
     * @return a new Money instance with the product
     */
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    /**
     * Multiplies this amount by an integer factor.
     *
     * @param factor the multiplication factor
     * @return a new Money instance with the product
     */
    public Money multiply(int factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    /**
     * Divides this amount by a divisor.
     *
     * @param divisor the divisor
     * @return a new Money instance with the quotient
     * @throws ArithmeticException if divisor is zero
     */
    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING), this.currency);
    }

    /**
     * Calculates percentage of this amount.
     *
     * @param percentage the percentage (e.g., 13 for 13%)
     * @return a new Money instance with the percentage amount
     */
    public Money percentage(BigDecimal percentage) {
        return multiply(percentage.divide(BigDecimal.valueOf(100)));
    }

    /**
     * Checks if this amount is greater than another.
     *
     * @param other the other amount
     * @return true if this amount is greater
     * @throws ErpBusinessException if currencies don't match
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this amount is less than another.
     *
     * @param other the other amount
     * @return true if this amount is less
     * @throws ErpBusinessException if currencies don't match
     */
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if this amount equals another.
     *
     * @param other the other amount
     * @return true if amounts are equal
     * @throws ErpBusinessException if currencies don't match
     */
    public boolean isEqualTo(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) == 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new ErpBusinessException("CURRENCY_MISMATCH",
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency));
        }
    }

    /**
     * Returns the amount as a plain decimal string.
     *
     * @return the amount string
     */
    public String getAmountAsString() {
        return amount.toPlainString();
    }

    /**
     * Returns the amount formatted for the currency.
     *
     * @return formatted amount
     */
    public String getFormattedAmount() {
        return String.format("%s %s", currency.getSymbol(), amount);
    }
}
