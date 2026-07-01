package com.erp.platform.validation.error;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a validation error.
 *
 * <p>This class encapsulates information about a validation failure,
 * including the field, error code, and message.
 *
 * @since 1.0.0
 */
public final class ValidationError {

    private final String field;
    private final ValidationErrorCode errorCode;
    private final String message;
    private final Object rejectedValue;
    private final List<Object> arguments;

    private ValidationError(String field, ValidationErrorCode errorCode, String message,
                           Object rejectedValue, List<Object> arguments) {
        this.field = field;
        this.errorCode = errorCode;
        this.message = message;
        this.rejectedValue = rejectedValue;
        this.arguments = arguments != null ? arguments : Collections.emptyList();
    }

    /**
     * Creates a new validation error.
     *
     * @param field the field name
     * @param errorCode the error code
     * @param message the error message
     * @return the validation error
     */
    public static ValidationError of(String field, ValidationErrorCode errorCode, String message) {
        return new ValidationError(field, errorCode, message, null, null);
    }

    /**
     * Creates a new validation error with rejected value.
     *
     * @param field the field name
     * @param errorCode the error code
     * @param message the error message
     * @param rejectedValue the rejected value
     * @return the validation error
     */
    public static ValidationError of(String field, ValidationErrorCode errorCode, String message,
                                     Object rejectedValue) {
        return new ValidationError(field, errorCode, message, rejectedValue, null);
    }

    /**
     * Creates a new validation error with arguments.
     *
     * @param field the field name
     * @param errorCode the error code
     * @param message the error message
     * @param arguments the arguments
     * @return the validation error
     */
    public static ValidationError of(String field, ValidationErrorCode errorCode, String message,
                                     List<Object> arguments) {
        return new ValidationError(field, errorCode, message, null, arguments);
    }

    /**
     * Returns the field name.
     *
     * @return the field name
     */
    public String field() {
        return field;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public ValidationErrorCode errorCode() {
        return errorCode;
    }

    /**
     * Returns the error message.
     *
     * @return the error message
     */
    public String message() {
        return message;
    }

    /**
     * Returns the rejected value.
     *
     * @return the rejected value
     */
    public Object rejectedValue() {
        return rejectedValue;
    }

    /**
     * Returns the arguments.
     *
     * @return the arguments
     */
    public List<Object> arguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return Objects.equals(field, that.field) &&
               errorCode == that.errorCode &&
               Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, errorCode, message);
    }

    @Override
    public String toString() {
        return "ValidationError{" +
               "field='" + field + '\'' +
               ", errorCode=" + errorCode +
               ", message='" + message + '\'' +
               '}';
    }
}
