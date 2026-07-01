package com.erp.platform.observability.logging;

import java.util.Map;

/**
 * Structured logging utilities for the ERP AI Platform.
 *
 * <p>This class provides utilities for structured logging
 * with consistent field names and formats.
 *
 * @since 1.0.0
 */
public final class StructuredLoggingUtils {

    private StructuredLoggingUtils() {
        // Utility class
    }

    /**
     * Creates a log message map with standard fields.
     *
     * @param message the log message
     * @return the log message map
     */
    public static Map<String, Object> logMessage(String message) {
        return Map.of(
            "message", message,
            "timestamp", java.time.Instant.now().toString()
        );
    }

    /**
     * Creates a log message map with standard fields and additional data.
     *
     * @param message the log message
     * @param additionalData additional data to include
     * @return the log message map
     */
    @SafeVarargs
    public static Map<String, Object> logMessage(String message, Map.Entry<String, Object>... additionalData) {
        Map<String, Object> logMap = logMessage(message);
        for (Map.Entry<String, Object> entry : additionalData) {
            logMap.put(entry.getKey(), entry.getValue());
        }
        return logMap;
    }

    /**
     * Creates a log entry for an error.
     *
     * @param message the error message
     * @param error the error
     * @return the log entry map
     */
    public static Map<String, Object> errorLog(String message, Throwable error) {
        return Map.of(
            "level", "ERROR",
            "message", message,
            "error", error.getClass().getName(),
            "errorMessage", error.getMessage(),
            "timestamp", java.time.Instant.now().toString()
        );
    }
}
