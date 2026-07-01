package com.erp.platform.observability.logging;

import com.erp.platform.observability.correlation.CorrelationId;
import com.erp.platform.common.context.RequestContext;

import java.time.Instant;

/**
 * Structured logging utilities.
 *
 * <p>Provides structured logging helpers that automatically include
 * correlation IDs, tenant IDs, and other context information.
 *
 * @since 1.0.0
 */
public final class StructuredLogger {

    private StructuredLogger() {
        // Utility class
    }

    /**
     * Creates a structured log message with standard fields.
     *
     * @param level the log level
     * @param message the log message
     * @param args message arguments
     * @return the structured log entry
     */
    public static LogEntry log(LogLevel level, String message, Object... args) {
        return new LogEntry(level, String.format(message, args))
            .withCorrelationId(CorrelationId.get())
            .withRequestId(RequestContext.getRequestId())
            .withTimestamp(Instant.now());
    }

    /**
     * Creates an info log entry.
     *
     * @param message the log message
     * @param args message arguments
     * @return the log entry
     */
    public static LogEntry info(String message, Object... args) {
        return log(LogLevel.INFO, message, args);
    }

    /**
     * Creates a warn log entry.
     *
     * @param message the log message
     * @param args message arguments
     * @return the log entry
     */
    public static LogEntry warn(String message, Object... args) {
        return log(LogLevel.WARN, message, args);
    }

    /**
     * Creates an error log entry.
     *
     * @param message the log message
     * @param args message arguments
     * @return the log entry
     */
    public static LogEntry error(String message, Object... args) {
        return log(LogLevel.ERROR, message, args);
    }

    /**
     * Creates a debug log entry.
     *
     * @param message the log message
     * @param args message arguments
     * @return the log entry
     */
    public static LogEntry debug(String message, Object... args) {
        return log(LogLevel.DEBUG, message, args);
    }

    /**
     * Log level enum.
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    /**
     * Structured log entry.
     */
    public static class LogEntry {
        private final LogLevel level;
        private final String message;
        private String correlationId;
        private String requestId;
        private Long tenantId;
        private Instant timestamp;
        private final java.util.Map<String, Object> fields = new java.util.HashMap<>();

        public LogEntry(LogLevel level, String message) {
            this.level = level;
            this.message = message;
        }

        public LogEntry withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public LogEntry withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public LogEntry withTenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public LogEntry withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public LogEntry withField(String key, Object value) {
            this.fields.put(key, value);
            return this;
        }

        public LogLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public String getRequestId() {
            return requestId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public java.util.Map<String, Object> getFields() {
            return fields;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s [correlationId=%s, requestId=%s, tenantId=%s] %s",
                level, timestamp, correlationId, requestId, tenantId, message);
        }
    }
}
