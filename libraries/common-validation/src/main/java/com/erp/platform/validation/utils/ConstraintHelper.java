package com.erp.platform.validation.utils;

import com.erp.platform.validation.error.ValidationError;
import com.erp.platform.validation.error.ValidationErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for building validation constraints.
 *
 * <p>This class provides helper methods for creating common
 * validation constraints.
 *
 * @since 1.0.0
 */
public final class ConstraintHelper {

    private ConstraintHelper() {
        // Utility class
    }

    /**
     * Creates a required field constraint.
     *
     * @param fieldName the field name
     * @return the validation error
     */
    public static ValidationError required(String fieldName) {
        return ValidationError.of(fieldName, ValidationErrorCode.REQUIRED,
            "%s is required".formatted(fieldName));
    }

    /**
     * Creates a not blank constraint.
     *
     * @param fieldName the field name
     * @return the validation error
     */
    public static ValidationError notBlank(String fieldName) {
        return ValidationError.of(fieldName, ValidationErrorCode.NOT_BLANK,
            "%s must not be blank".formatted(fieldName));
    }

    /**
     * Creates a not null constraint.
     *
     * @param fieldName the field name
     * @return the validation error
     */
    public static ValidationError notNull(String fieldName) {
        return ValidationError.of(fieldName, ValidationErrorCode.NOT_NULL,
            "%s must not be null".formatted(fieldName));
    }

    /**
     * Creates a size constraint.
     *
     * @param fieldName the field name
     * @param min the minimum size
     * @param max the maximum size
     * @return the validation error
     */
    public static ValidationError size(String fieldName, int min, int max) {
        return ValidationError.of(fieldName, ValidationErrorCode.SIZE,
            "%s size must be between %d and %d".formatted(fieldName, min, max));
    }

    /**
     * Creates a pattern constraint.
     *
     * @param fieldName the field name
     * @param pattern the pattern
     * @return the validation error
     */
    public static ValidationError pattern(String fieldName, String pattern) {
        return ValidationError.of(fieldName, ValidationErrorCode.PATTERN,
            "%s must match pattern %s".formatted(fieldName, pattern));
    }

    /**
     * Creates a custom validation error.
     *
     * @param fieldName the field name
     * @param message the error message
     * @return the validation error
     */
    public static ValidationError custom(String fieldName, String message) {
        return ValidationError.of(fieldName, ValidationErrorCode.CUSTOM, message);
    }
}
