package com.erp.platform.observability.correlation;

import java.util.UUID;

/**
 * Correlation ID utilities.
 *
 * <p>Provides utilities for generating and managing correlation IDs
 * for distributed tracing and request tracking.
 *
 * @since 1.0.0
 */
public final class CorrelationId {

    private CorrelationId() {
        // Utility class
    }

    private static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
    private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();

    /**
     * Generates a new correlation ID.
     *
     * @return a new correlation ID
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sets the current correlation ID.
     *
     * @param correlationId the correlation ID
     */
    public static void set(String correlationId) {
        CURRENT_CORRELATION_ID.set(correlationId);
    }

    /**
     * Returns the current correlation ID.
     *
     * @return the correlation ID, or null if not set
     */
    public static String get() {
        return CURRENT_CORRELATION_ID.get();
    }

    /**
     * Returns the current correlation ID, generating one if not set.
     *
     * @return the correlation ID
     */
    public static String getOrGenerate() {
        String correlationId = CURRENT_CORRELATION_ID.get();
        if (correlationId == null) {
            correlationId = generate();
            set(correlationId);
        }
        return correlationId;
    }

    /**
     * Clears the current correlation ID.
     */
    public static void clear() {
        CURRENT_CORRELATION_ID.remove();
    }
}
