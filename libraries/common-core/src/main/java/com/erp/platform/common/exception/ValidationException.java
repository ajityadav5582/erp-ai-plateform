package com.erp.platform.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when validation fails.
 *
 * <p>Contains field-level validation errors for detailed error reporting.
 *
 * @since 1.0.0
 */
public class ValidationException extends ErpException {

    private final List<FieldError> fieldErrors;

    /**
     * Creates a new validation exception.
     *
     * @param message the error message
     * @param fieldErrors the field-level errors
     */
    public ValidationException(String message, List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", message, Map.of("fieldErrors", fieldErrors));
        this.fieldErrors = fieldErrors;
    }

    /**
     * Creates a new validation exception.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = List.of();
    }

    /**
     * Returns the field-level errors.
     */
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Field-level validation error.
     */
    public record FieldError(
        String field,
        String message,
        Object rejectedValue
    ) {}
}
