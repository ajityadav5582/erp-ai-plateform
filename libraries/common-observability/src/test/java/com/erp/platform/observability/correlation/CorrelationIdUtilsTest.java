package com.erp.platform.observability.correlation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CorrelationIdUtils}.
 */
class CorrelationIdUtilsTest {

    @Test
    @DisplayName("Should generate valid correlation ID")
    void shouldGenerateValidCorrelationId() {
        // When
        String correlationId = CorrelationIdUtils.generate();

        // Then
        assertThat(correlationId).isNotNull();
        assertThat(CorrelationIdUtils.isValid(correlationId)).isTrue();
    }

    @Test
    @DisplayName("Should validate valid correlation ID")
    void shouldValidateValidCorrelationId() {
        // Given
        String validId = UUID.randomUUID().toString();

        // When
        boolean isValid = CorrelationIdUtils.isValid(validId);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate null correlation ID")
    void shouldInvalidateNullCorrelationId() {
        // When
        boolean isValid = CorrelationIdUtils.isValid(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate blank correlation ID")
    void shouldInvalidateBlankCorrelationId() {
        // When
        boolean isValid = CorrelationIdUtils.isValid("   ");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate non-UUID correlation ID")
    void shouldInvalidateNonUuidCorrelationId() {
        // When
        boolean isValid = CorrelationIdUtils.isValid("not-a-uuid");

        // Then
        assertThat(isValid).isFalse();
    }
}
