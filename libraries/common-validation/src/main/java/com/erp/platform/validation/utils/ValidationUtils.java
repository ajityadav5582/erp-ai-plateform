package com.erp.platform.validation.utils;

import com.erp.platform.validation.error.ValidationError;
import com.erp.platform.validation.error.ValidationErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validation utility methods.
 *
 * <p>This class provides utility methods for common validation operations.
 *
 * @since 1.0.0
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

    private static final Pattern NEPALI_PHONE_PATTERN = Pattern.compile("^(\\+977|0)[0-9]{9,10}$");

    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-z0-9-]+$");

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Validates that a string is not blank.
     *
     * @param value the value to validate
     * @param fieldName the field name
     * @return empty list if valid, list of errors if invalid
     */
    public static List<ValidationError> validateNotBlank(String value, String fieldName) {
        List<ValidationError> errors = new ArrayList<>();
        if (value == null || value.isBlank()) {
            errors.add(ValidationError.of(fieldName, ValidationErrorCode.NOT_BLANK,
                "%s must not be blank".formatted(fieldName)));
        }
        return errors;
    }

    /**
     * Validates that a value is not negative.
     *
     * @param value the value to validate
     * @param fieldName the field name
     * @return empty list if valid, list of errors if invalid
     */
    public static List<ValidationError> validateNotNegative(Number value, String fieldName) {
        List<ValidationError> errors = new ArrayList<>();
        if (value != null && value.doubleValue() < 0) {
            errors.add(ValidationError.of(fieldName, ValidationErrorCode.NOT_NEGATIVE,
                "%s must not be negative".formatted(fieldName)));
        }
        return errors;
    }

    /**
     * Validates that a value is positive.
     *
     * @param value the value to validate
     * @param fieldName the field name
     * @return empty list if valid, list of errors if invalid
     */
    public static List<ValidationError> validatePositive(Number value, String fieldName) {
        List<ValidationError> errors = new ArrayList<>();
        if (value != null && value.doubleValue() <= 0) {
            errors.add(ValidationError.of(fieldName, ValidationErrorCode.POSITIVE,
                "%s must be positive".formatted(fieldName)));
        }
        return errors;
    }

    /**
     * Validates an email address.
     *
     * @param email the email to validate
     * @param fieldName the field name
     * @return empty list if valid, list of errors if invalid
     */
    public static List<ValidationError> validateEmail(String email, String fieldName) {
        List<ValidationError> errors = new ArrayList<>();
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add(ValidationError.of(fieldName, ValidationErrorCode.EMAIL,
                "%s must be a valid email address".formatted(fieldName)));
        }
        return errors;
    }

    /**
     * Validates a phone number.
     *
     * @param phone the phone to validate
     * @param fieldName the field name
     * @return empty list if valid, list of errors if invalid
     */
    public static List<ValidationError> validatePhone(String phone, String fieldName) {
        List<ValidationError> errors = new ArrayList<>();
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(ValidationError.of(fieldName, ValidationErrorCode.PHONE,
                "%s must be a valid phone number".formatted(fieldName)));
        }
        return errors;
    }

    /**
     * Validates a Nepali phone number.
     *
     * @param phone the phone to validate
     * @param fieldName the field name
     * @return empty list if valid, list of errors if invalid
     */
    public static List<ValidationError> validateNepaliPhone(String phone, String fieldName) {
        List<ValidationError> errors = new ArrayList<>();
        if (phone != null && !phone.isBlank() && !NEPALI_PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(ValidationError.of(fieldName, ValidationErrorCode.NEPALI_PHONE,
                "%s must be a valid Nepali phone number".formatted(fieldName)));
        }
        return errors;
    }

    /**
     * Validates a tenant ID.
     *
     * @param tenantId the tenant ID to validate
     * @param fieldName the field name
     * @return empty list if valid, list of errors if invalid
     */
    public static List<ValidationError> validateTenantId(String tenantId, String fieldName) {
        List<ValidationError> errors = new ArrayList<>();
        if (tenantId != null && !tenantId.isBlank() && !TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            errors.add(ValidationError.of(fieldName, ValidationErrorCode.TENANT_ID,
                "%s must be a valid tenant ID (lowercase letters, numbers, hyphens)".formatted(fieldName)));
        }
        return errors;
    }

    /**
     * Combines multiple lists of validation errors.
     *
     * @param errorLists the lists of errors
     * @return combined list of errors
     */
    @SafeVarargs
    public static List<ValidationError> combine(List<ValidationError>... errorLists) {
        List<ValidationError> combined = new ArrayList<>();
        for (List<ValidationError> errors : errorLists) {
            if (errors != null) {
                combined.addAll(errors);
            }
        }
        return combined;
    }
}
