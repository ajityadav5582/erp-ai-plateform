package com.erp.platform.observability.correlation;

import java.util.UUID;

/**
 * Correlation ID utilities for the ERP AI Platform.
 *
 * <p>This class provides utilities for generating and managing
 * correlation IDs for request tracing.
 *
 * @since 1.0.0
 */
public final class CorrelationIdUtils {

    /**
     * HTTP header name for correlation ID.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * MDC key for correlation ID.
     */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private CorrelationIdUtils() {
        // Utility class
    }

    /**
     * Generates a new correlation ID.
     *
     * @return a new correlation ID
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validates a correlation ID.
     *
     * @param correlationId the correlation ID to validate
     * @return true if valid
     */
    public static boolean isValid(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(correlationId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns the correlation ID from the current MDC context.
     *
     * @return the correlation ID, or null if not present
     */
    public static String fromMdc() {
        // This will be implemented by the platform service
        // using the actual MDC implementation
        return null;
    }
}
