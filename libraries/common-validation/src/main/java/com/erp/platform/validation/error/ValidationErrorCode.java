package com.erp.platform.validation.error;

/**
 * Validation error codes for the ERP AI Platform.
 *
 * <p>This enum provides standardized error codes for validation failures.
 *
 * @since 1.0.0
 */
public enum ValidationErrorCode {

    /**
     * Field is required.
     */
    REQUIRED("VALIDATION_REQUIRED"),

    /**
     * Field is not blank.
     */
    NOT_BLANK("VALIDATION_NOT_BLANK"),

    /**
     * Field is not null.
     */
    NOT_NULL("VALIDATION_NOT_NULL"),

    /**
     * Field is not negative.
     */
    NOT_NEGATIVE("VALIDATION_NOT_NEGATIVE"),

    /**
     * Field must be positive.
     */
    POSITIVE("VALIDATION_POSITIVE"),

    /**
     * Field must be a valid email.
     */
    EMAIL("VALIDATION_EMAIL"),

    /**
     * Field must be a valid phone number.
     */
    PHONE("VALIDATION_PHONE"),

    /**
     * Field must be a valid Nepali phone number.
     */
    NEPALI_PHONE("VALIDATION_NEPALI_PHONE"),

    /**
     * Field must be a valid tenant ID.
     */
    TENANT_ID("VALIDATION_TENANT_ID"),

    /**
     * Field must be a valid UUID.
     */
    UUID("VALIDATION_UUID"),

    /**
     * Field must be a valid date.
     */
    DATE("VALIDATION_DATE"),

    /**
     * Field must be a valid date-time.
     */
    DATE_TIME("VALIDATION_DATE_TIME"),

    /**
     * Field size is out of range.
     */
    SIZE("VALIDATION_SIZE"),

    /**
     * Field length is out of range.
     */
    LENGTH("VALIDATION_LENGTH"),

    /**
     * Field pattern does not match.
     */
    PATTERN("VALIDATION_PATTERN"),

    /**
     * Custom validation error.
     */
    CUSTOM("VALIDATION_CUSTOM");

    private final String code;

    ValidationErrorCode(String code) {
        this.code = code;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public String code() {
        return code;
    }
}
