package com.erp.platform.validation.utils;

import com.erp.platform.validation.error.ValidationError;
import com.erp.platform.validation.error.ValidationErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidationUtils}.
 */
class ValidationUtilsTest {

    @Test
    @DisplayName("Should return error for null value in validateNotBlank")
    void shouldReturnErrorForNullValueInValidateNotBlank() {
        // When
        List<ValidationError> errors = ValidationUtils.validateNotBlank(null, "field");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).field()).isEqualTo("field");
        assertThat(errors.get(0).errorCode()).isEqualTo(ValidationErrorCode.NOT_BLANK);
    }

    @Test
    @DisplayName("Should return error for blank value in validateNotBlank")
    void shouldReturnErrorForBlankValueInValidateNotBlank() {
        // When
        List<ValidationError> errors = ValidationUtils.validateNotBlank("   ", "field");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorCode()).isEqualTo(ValidationErrorCode.NOT_BLANK);
    }

    @Test
    @DisplayName("Should return no errors for valid value in validateNotBlank")
    void shouldReturnNoErrorsForValidValueInValidateNotBlank() {
        // When
        List<ValidationError> errors = ValidationUtils.validateNotBlank("value", "field");

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should return error for negative value in validateNotNegative")
    void shouldReturnErrorForNegativeValueInValidateNotNegative() {
        // When
        List<ValidationError> errors = ValidationUtils.validateNotNegative(-1, "field");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorCode()).isEqualTo(ValidationErrorCode.NOT_NEGATIVE);
    }

    @Test
    @DisplayName("Should return no errors for zero in validateNotNegative")
    void shouldReturnNoErrorsForZeroInValidateNotNegative() {
        // When
        List<ValidationError> errors = ValidationUtils.validateNotNegative(0, "field");

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should return error for non-positive value in validatePositive")
    void shouldReturnErrorForNonPositiveValueInValidatePositive() {
        // When
        List<ValidationError> errors = ValidationUtils.validatePositive(0, "field");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorCode()).isEqualTo(ValidationErrorCode.POSITIVE);
    }

    @Test
    @DisplayName("Should return error for invalid email")
    void shouldReturnErrorForInvalidEmail() {
        // When
        List<ValidationError> errors = ValidationUtils.validateEmail("invalid", "field");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorCode()).isEqualTo(ValidationErrorCode.EMAIL);
    }

    @Test
    @DisplayName("Should return no errors for valid email")
    void shouldReturnNoErrorsForValidEmail() {
        // When
        List<ValidationError> errors = ValidationUtils.validateEmail("test@example.com", "field");

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should return error for invalid Nepali phone")
    void shouldReturnErrorForInvalidNepaliPhone() {
        // When
        List<ValidationError> errors = ValidationUtils.validateNepaliPhone("123456", "field");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorCode()).isEqualTo(ValidationErrorCode.NEPALI_PHONE);
    }

    @Test
    @DisplayName("Should return no errors for valid Nepali phone")
    void shouldReturnNoErrorsForValidNepaliPhone() {
        // When
        List<ValidationError> errors = ValidationUtils.validateNepaliPhone("+9779841234567", "field");

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should return error for invalid tenant ID")
    void shouldReturnErrorForInvalidTenantId() {
        // When
        List<ValidationError> errors = ValidationUtils.validateTenantId("INVALID_TENANT", "field");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorCode()).isEqualTo(ValidationErrorCode.TENANT_ID);
    }

    @Test
    @DisplayName("Should return no errors for valid tenant ID")
    void shouldReturnNoErrorsForValidTenantId() {
        // When
        List<ValidationError> errors = ValidationUtils.validateTenantId("tenant-123", "field");

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should combine multiple error lists")
    void shouldCombineMultipleErrorLists() {
        // When
        List<ValidationError> errors = ValidationUtils.combine(
            ValidationUtils.validateNotBlank(null, "field1"),
            ValidationUtils.validateEmail("invalid", "field2")
        );

        // Then
        assertThat(errors).hasSize(2);
    }
}
