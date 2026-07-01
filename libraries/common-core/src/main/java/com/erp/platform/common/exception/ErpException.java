package com.erp.platform.common.exception;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Base exception for all ERP platform exceptions.
 *
 * <p>All application-specific exceptions should extend this class.
 * It provides a consistent error structure with error codes and details.
 *
 * @since 1.0.0
 */
public abstract class ErpException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final Map<String, Object> details;
    private final Instant timestamp;

    /**
     * Creates a new ERP exception.
     *
     * @param errorCode the error code
     * @param message the error message
     */
    protected ErpException(String errorCode, String message) {
        this(errorCode, message, null, null);
    }

    /**
     * Creates a new ERP exception.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param details additional error details
     */
    protected ErpException(String errorCode, String message, Map<String, Object> details) {
        this(errorCode, message, details, null);
    }

    /**
     * Creates a new ERP exception.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param cause the root cause
     */
    protected ErpException(String errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    /**
     * Creates a new ERP exception.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param details additional error details
     * @param cause the root cause
     */
    protected ErpException(String errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
        this.timestamp = Instant.now();
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns additional error details.
     *
     * @return the error details, or null if none
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * Returns the timestamp when the exception occurred.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a detail value by key.
     *
     * @param key the detail key
     * @param <T> the detail type
     * @return the detail value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key) {
        if (details == null) {
            return null;
        }
        return (T) details.get(key);
    }
}
