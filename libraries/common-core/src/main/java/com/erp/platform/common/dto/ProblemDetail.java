package com.erp.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RFC 7807 Problem Details implementation for error responses.
 *
 * <p>Provides a standardized error response format following RFC 7807.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7807">RFC 7807</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "RFC 7807 Problem Details")
public record ProblemDetail(
    @Schema(description = "Error type URI", example = "https://api.erp-platform.com/errors/validation")
    String type,

    @Schema(description = "Error title", example = "Validation Error")
    String title,

    @Schema(description = "HTTP status code", example = "422")
    Integer status,

    @Schema(description = "Error detail", example = "Request validation failed")
    String detail,

    @Schema(description = "Request path", example = "/api/v1/invoices")
    String instance,

    @Schema(description = "Error timestamp", example = "2024-01-15T10:30:00Z")
    Instant timestamp,

    @Schema(description = "Request correlation ID", example = "req-abc123")
    String requestId,

    @Schema(description = "Additional error details")
    Map<String, Object> details,

    @Schema(description = "Field-level validation errors")
    List<FieldError> fieldErrors
) implements Serializable {

    /**
     * Creates a Problem Detail with minimal information.
     */
    public static ProblemDetail of(String title, String detail, int status) {
        return new ProblemDetail(
            null,
            title,
            status,
            detail,
            null,
            Instant.now(),
            null,
            null,
            null
        );
    }

    /**
     * Creates a Problem Detail with all fields.
     */
    public static ProblemDetail of(String type, String title, String detail, int status,
                                    String instance, String requestId) {
        return new ProblemDetail(
            type,
            title,
            status,
            detail,
            instance,
            Instant.now(),
            requestId,
            null,
            null
        );
    }

    /**
     * Field-level validation error.
     *
     * @param field the field name
     * @param message the error message
     * @param rejectedValue the rejected value
     */
    public record FieldError(
        @Schema(description = "Field name", example = "email")
        String field,

        @Schema(description = "Error message", example = "Email format is invalid")
        String message,

        @Schema(description = "Rejected value", example = "invalid-email")
        Object rejectedValue
    ) implements Serializable {
    }
}
