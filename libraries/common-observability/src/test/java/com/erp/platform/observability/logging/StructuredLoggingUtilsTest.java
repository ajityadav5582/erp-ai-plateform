package com.erp.platform.observability.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StructuredLoggingUtils}.
 */
class StructuredLoggingUtilsTest {

    @Test
    @DisplayName("Should create log message with standard fields")
    void shouldCreateLogMessageWithStandardFields() {
        // When
        Map<String, Object> logMessage = StructuredLoggingUtils.logMessage("test message");

        // Then
        assertThat(logMessage).containsEntry("message", "test message");
        assertThat(logMessage).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should create log message with additional data")
    void shouldCreateLogMessageWithAdditionalData() {
        // When
        Map<String, Object> logMessage = StructuredLoggingUtils.logMessage(
            "test message",
            Map.entry("key1", "value1"),
            Map.entry("key2", "value2")
        );

        // Then
        assertThat(logMessage).containsEntry("message", "test message");
        assertThat(logMessage).containsEntry("key1", "value1");
        assertThat(logMessage).containsEntry("key2", "value2");
    }

    @Test
    @DisplayName("Should create error log")
    void shouldCreateErrorLog() {
        // Given
        Exception error = new RuntimeException("test error");

        // When
        Map<String, Object> errorLog = StructuredLoggingUtils.errorLog("error occurred", error);

        // Then
        assertThat(errorLog).containsEntry("level", "ERROR");
        assertThat(errorLog).containsEntry("message", "error occurred");
        assertThat(errorLog).containsEntry("error", "java.lang.RuntimeException");
        assertThat(errorLog).containsEntry("errorMessage", "test error");
    }
}
