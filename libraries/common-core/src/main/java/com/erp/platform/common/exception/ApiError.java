package com.erp.platform.common.exception;

import java.time.Instant;

/**
 * Standard error response body shared by every platform REST service.
 *
 * <p>Previously this record was nested inside the Identity
 * {@code GlobalExceptionHandler}, which meant each service had to redeclare it and
 * the wire format could silently drift between services. It now lives here so all
 * services emit an identical error shape.
 *
 * <p>The field names and order are part of the public API contract and are consumed
 * by the frontend error handler, so they must not be renamed.
 *
 * @param code      machine-readable error code
 * @param message   human-readable message
 * @param path      the request path that produced the error
 * @param timestamp occurrence time
 * @since 1.0.0
 */
public record ApiError(String code, String message, String path, Instant timestamp) {
}
