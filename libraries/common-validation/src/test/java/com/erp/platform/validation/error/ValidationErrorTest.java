package com.erp.platform.validation.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidationError}.
 */
class ValidationErrorTest {

    @Test
    @DisplayName("Should create validation error with of()")
    void shouldCreateValidationErrorWithOf() {
        // When
        ValidationError error = ValidationError.of("field", ValidationErrorCode.REQUIRED, "is required");

        // Then
        assertThat(error.field()).isEqualTo("field");
        assertThat(error.errorCode()).isEqualTo(ValidationErrorCode.REQUIRED);
        assertThat(error.message()).isEqualTo("is required");
        assertThat(error.rejectedValue()).isNull();
        assertThat(error.arguments()).isEmpty();
    }

    @Test
    @DisplayName("Should create validation error with rejected value")
    void shouldCreateValidationErrorWithRejectedValue() {
        // When
        ValidationError error = ValidationError.of("field", ValidationErrorCode.PATTERN, "invalid", "bad");

        // Then
        assertThat(error.rejectedValue()).isEqualTo("bad");
    }

    @Test
    @DisplayName("Should create validation error with arguments")
    void shouldCreateValidationErrorWithArguments() {
        // When
        ValidationError error = ValidationError.of("field", ValidationErrorCode.SIZE, "size error", List.of(1, 10));

        // Then
        assertThat(error.arguments()).hasSize(2);
        assertThat(error.arguments().get(0)).isEqualTo(1);
        assertThat(error.arguments().get(1)).isEqualTo(10);
    }

    @Test
    @DisplayName("Should be equal for same field and error code")
    void shouldBeEqualForSameFieldAndErrorCode() {
        // Given
        ValidationError error1 = ValidationError.of("field", ValidationErrorCode.REQUIRED, "msg1");
        ValidationError error2 = ValidationError.of("field", ValidationErrorCode.REQUIRED, "msg2");

        // Then
        assertThat(error1).isEqualTo(error2);
        assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
    }
}
