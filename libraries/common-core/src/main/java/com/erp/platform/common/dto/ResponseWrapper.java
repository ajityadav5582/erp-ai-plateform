package com.erp.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.Instant;

/**
 * Standard response wrapper for all API responses.
 *
 * <p>Provides a consistent response structure across all endpoints
 * with metadata for tracing and debugging.
 *
 * @param <T> the type of the response data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public record ResponseWrapper<T>(
    @Schema(description = "Response data", example = "null")
    T data,

    @Schema(description = "Response metadata")
    Meta meta
) implements Serializable {

    /**
     * Creates a successful response with data.
     *
     * @param data the response data
     * @param <T> the type of the response data
     * @return a new ResponseWrapper
     */
    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(data, Meta.success());
    }

    /**
     * Creates a response with data and custom metadata.
     *
     * @param data the response data
     * @param meta the response metadata
     * @param <T> the type of the response data
     * @return a new ResponseWrapper
     */
    public static <T> ResponseWrapper<T> of(T data, Meta meta) {
        return new ResponseWrapper<>(data, meta);
    }

    /**
     * Response metadata.
     *
     * @param requestId the request correlation ID
     * @param timestamp the response timestamp
     * @param traceId the distributed trace ID
     * @param spanId the current span ID
     */
    public record Meta(
        @Schema(description = "Request correlation ID", example = "req-abc123")
        String requestId,

        @Schema(description = "Response timestamp", example = "2024-01-15T10:30:00Z")
        Instant timestamp,

        @Schema(description = "Distributed trace ID", example = "4bf92f3577b34da6a3ce929d0e0e4736")
        String traceId,

        @Schema(description = "Current span ID", example = "00f067aa0ba902b7")
        String spanId
    ) implements Serializable {
        /**
         * Creates success metadata with current timestamp.
         */
        public static Meta success() {
            return new Meta(null, Instant.now(), null, null);
        }

        /**
         * Creates metadata with all fields.
         */
        public static Meta of(String requestId, Instant timestamp, String traceId, String spanId) {
            return new Meta(requestId, timestamp, traceId, spanId);
        }
    }
}
