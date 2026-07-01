package com.erp.platform.common.model.vo;

import com.erp.platform.common.exception.ErpBusinessException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Money}.
 *
 * @since 1.0.0
 */
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void of_shouldCreateMoneyFromString() {
        Money money = Money.of("100.50", USD);

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    void of_shouldCreateMoneyFromDouble() {
        Money money = Money.of(100.50, USD);

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    void zero_shouldCreateZeroMoney() {
        Money money = Money.zero(USD);

        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    void add_shouldAddMoneyWithSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("50.00"), USD);

        Money result = money1.add(money2);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    void add_shouldThrowForCurrencyMismatch() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("50.00"), EUR);

        assertThatThrownBy(() -> money1.add(money2))
            .isInstanceOf(ErpBusinessException.class)
            .hasMessageContaining("Currency mismatch");
    }

    @Test
    void subtract_shouldSubtractMoneyWithSameCurrency() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("30.00"), USD);

        Money result = money1.subtract(money2);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void subtract_shouldThrowForNegativeResult() {
        Money money1 = new Money(new BigDecimal("30.00"), USD);
        Money money2 = new Money(new BigDecimal("100.00"), USD);

        assertThatThrownBy(() -> money1.subtract(money2))
            .isInstanceOf(ErpBusinessException.class)
            .hasMessageContaining("Negative amount");
    }

    @Test
    void multiply_shouldMultiplyByBigDecimal() {
        Money money = new Money(new BigDecimal("100.00"), USD);

        Money result = money.multiply(new BigDecimal("1.5"));

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void multiply_shouldMultiplyByInt() {
        Money money = new Money(new BigDecimal("100.00"), USD);

        Money result = money.multiply(3);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void divide_shouldDivideByBigDecimal() {
        Money money = new Money(new BigDecimal("100.00"), USD);

        Money result = money.divide(new BigDecimal("4"));

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void divide_shouldThrowForZeroDivisor() {
        Money money = new Money(new BigDecimal("100.00"), USD);

        assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void percentage_shouldCalculatePercentage() {
        Money money = new Money(new BigDecimal("100.00"), USD);

        Money result = money.percentage(new BigDecimal("13"));

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("13.00"));
    }

    @Test
    void isGreaterThan_shouldCompareCorrectly() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("50.00"), USD);

        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money2.isGreaterThan(money1)).isFalse();
    }

    @Test
    void isLessThan_shouldCompareCorrectly() {
        Money money1 = new Money(new BigDecimal("50.00"), USD);
        Money money2 = new Money(new BigDecimal("100.00"), USD);

        assertThat(money1.isLessThan(money2)).isTrue();
        assertThat(money2.isLessThan(money1)).isFalse();
    }

    @Test
    void isEqualTo_shouldCompareCorrectly() {
        Money money1 = new Money(new BigDecimal("100.00"), USD);
        Money money2 = new Money(new BigDecimal("100.00"), USD);

        assertThat(money1.isEqualTo(money2)).isTrue();
    }

    @Test
    void getAmountAsString_shouldReturnPlainString() {
        Money money = new Money(new BigDecimal("100.50"), USD);

        assertThat(money.getAmountAsString()).isEqualTo("100.50");
    }

    @Test
    void getFormattedAmount_shouldFormatWithCurrencySymbol() {
        Money money = new Money(new BigDecimal("100.50"), USD);

        assertThat(money.getFormattedAmount()).isEqualTo("$100.50");
    }

    @Test
    void constructor_shouldNormalizeScale() {
        Money money = new Money(new BigDecimal("100.123456"), USD);

        assertThat(money.amount().scale()).isEqualTo(4);
    }

    @Test
    void constructor_shouldThrowForNullAmount() {
        assertThatThrownBy(() -> new Money(null, USD))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_shouldThrowForNullCurrency() {
        assertThatThrownBy(() -> new Money(new BigDecimal("100.00"), null))
            .isInstanceOf(NullPointerException.class);
    }
}
