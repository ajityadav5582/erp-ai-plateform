package com.erp.platform.observability.trace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TraceHelpers}.
 */
class TraceHelpersTest {

    @Test
    @DisplayName("Should generate valid trace ID")
    void shouldGenerateValidTraceId() {
        // When
        String traceId = TraceHelpers.generateTraceId();

        // Then
        assertThat(traceId).isNotNull();
        assertThat(TraceHelpers.isValidTraceId(traceId)).isTrue();
    }

    @Test
    @DisplayName("Should generate valid span ID")
    void shouldGenerateValidSpanId() {
        // When
        String spanId = TraceHelpers.generateSpanId();

        // Then
        assertThat(spanId).isNotNull();
    }

    @Test
    @DisplayName("Should validate valid trace ID")
    void shouldValidateValidTraceId() {
        // Given
        String validId = UUID.randomUUID().toString();

        // When
        boolean isValid = TraceHelpers.isValidTraceId(validId);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate null trace ID")
    void shouldInvalidateNullTraceId() {
        // When
        boolean isValid = TraceHelpers.isValidTraceId(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate non-UUID trace ID")
    void shouldInvalidateNonUuidTraceId() {
        // When
        boolean isValid = TraceHelpers.isValidTraceId("not-a-uuid");

        // Then
        assertThat(isValid).isFalse();
    }
}
