package com.erp.platform.observability.logging;

/**
 * Logging constants for the ERP AI Platform.
 *
 * <p>This class provides standardized constants for logging
 * across the platform.
 *
 * @since 1.0.0
 */
public final class LoggingConstants {

    /**
     * MDC key for correlation ID.
     */
    public static final String MDC_CORRELATION_ID = "correlationId";

    /**
     * MDC key for tenant ID.
     */
    public static final String MDC_TENANT_ID = "tenantId";

    /**
     * MDC key for user ID.
     */
    public static final String MDC_USER_ID = "userId";

    /**
     * MDC key for request ID.
     */
    public static final String MDC_REQUEST_ID = "requestId";

    /**
     * MDC key for span ID.
     */
    public static final String MDC_SPAN_ID = "spanId";

    /**
     * MDC key for trace ID.
     */
    public static final String MDC_TRACE_ID = "traceId";

    private LoggingConstants() {
        // Utility class
    }
}
