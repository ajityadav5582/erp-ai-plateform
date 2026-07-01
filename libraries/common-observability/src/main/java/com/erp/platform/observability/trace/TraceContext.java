package com.erp.platform.observability.trace;

import com.erp.platform.observability.correlation.CorrelationId;

/**
 * Distributed tracing context.
 *
 * <p>Provides utilities for managing trace and span IDs
 * for distributed tracing. This is an abstraction that can be
 * integrated with OpenTelemetry or other tracing frameworks.
 *
 * <p>Note: Actual OpenTelemetry implementation is deferred
 * to platform services. This provides the abstraction only.
 *
 * @since 1.0.0
 */
public final class TraceContext {

    private TraceContext() {
        // Utility class
    }

    private static final ThreadLocal<String> CURRENT_TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SPAN_ID = new ThreadLocal<>();

    /**
     * Sets the current trace and span IDs.
     *
     * @param traceId the trace ID
     * @param spanId the span ID
     */
    public static void set(String traceId, String spanId) {
        CURRENT_TRACE_ID.set(traceId);
        CURRENT_SPAN_ID.set(spanId);
    }

    /**
     * Returns the current trace ID.
     *
     * @return the trace ID, or null if not set
     */
    public static String getTraceId() {
        return CURRENT_TRACE_ID.get();
    }

    /**
     * Returns the current span ID.
     *
     * @return the span ID, or null if not set
     */
    public static String getSpanId() {
        return CURRENT_SPAN_ID.get();
    }

    /**
     * Clears the trace context.
     */
    public static void clear() {
        CURRENT_TRACE_ID.remove();
        CURRENT_SPAN_ID.remove();
    }

    /**
     * Creates a trace context from correlation ID.
     *
     * <p>In many systems, the correlation ID is used as the trace ID.
     *
     * @param correlationId the correlation ID
     */
    public static void fromCorrelationId(String correlationId) {
        if (correlationId != null) {
            CURRENT_TRACE_ID.set(correlationId);
        }
    }
}
