package com.erp.platform.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.util.Map;

/**
 * Base exception for every domain error that maps directly onto an HTTP response.
 *
 * <p>This is the single place where a domain failure declares <em>how</em> it should
 * be reported over HTTP. Previously each service repeated one
 * {@code @ExceptionHandler} method per exception type just to name a status and an
 * error code. Carrying both on the exception lets a shared
 * {@code GlobalExceptionHandler} collapse that boilerplate into one method.
 *
 * <p>Design notes:
 * <ul>
 *   <li>The {@link #errorCode} is supplied to {@link ErpException}, so no duplicate
 *       field is introduced here.</li>
 *   <li>Subclasses stay semantic. {@code CategoryNotFoundException} still exists and
 *       still means "category not found"; it simply no longer repeats constructor
 *       boilerplate.</li>
 *   <li>Extends {@link ErpException} rather than {@code RuntimeException} so these
 *       exceptions keep the platform's structured error contract (code, details,
 *       timestamp).</li>
 * </ul>
 *
 * @since 1.0.0
 */
public abstract class BaseApiException extends ErpException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    /**
     * Creates an API exception bound to an HTTP status and error code.
     *
     * @param status    the HTTP status to respond with
     * @param errorCode the machine-readable error code
     * @param message   the human-readable message
     */
    protected BaseApiException(HttpStatus status, String errorCode, String message) {
        super(errorCode, message);
        this.status = status;
    }

    /**
     * Creates an API exception bound to an HTTP status and error code, with a cause.
     *
     * @param status    the HTTP status to respond with
     * @param errorCode the machine-readable error code
     * @param message   the human-readable message
     * @param cause     the root cause
     */
    protected BaseApiException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.status = status;
    }

    /**
     * Creates an API exception bound to an HTTP status and error code, with details.
     *
     * @param status    the HTTP status to respond with
     * @param errorCode the machine-readable error code
     * @param message   the human-readable message
     * @param details   additional error details
     */
    protected BaseApiException(HttpStatus status, String errorCode, String message,
                               Map<String, Object> details) {
        super(errorCode, message, details);
        this.status = status;
    }

    /**
     * Returns the HTTP status this exception should be reported with.
     *
     * @return the HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }
}
