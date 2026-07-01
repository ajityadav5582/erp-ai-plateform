package com.erp.platform.observability.trace;

import java.util.UUID;

/**
 * Trace helpers for the ERP AI Platform.
 *
 * <p>This class provides utilities for distributed tracing.
 *
 * @since 1.0.0
 */
public final class TraceHelpers {

    /**
     * Trace header name.
     */
    public static final String TRACE_ID_HEADER = "X-Trace-ID";

    /**
     * Span header name.
     */
    public static final String SPAN_ID_HEADER = "X-Span-ID";

    private TraceHelpers() {
        // Utility class
    }

    /**
     * Generates a new trace ID.
     *
     * @return a new trace ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a new span ID.
     *
     * @return a new span ID
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validates a trace ID.
     *
     * @param traceId the trace ID to validate
     * @return true if valid
     */
    public static boolean isValidTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(traceId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
