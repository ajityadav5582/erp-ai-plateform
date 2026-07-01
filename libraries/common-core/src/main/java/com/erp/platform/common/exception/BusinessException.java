package com.erp.platform.common.exception;

import java.util.Map;

/**
 * Exception for business rule violations.
 *
 * <p>Thrown when a business rule is violated or a business constraint
 * is not met. This is a runtime exception that should be caught by
 * the global exception handler.
 *
 * @since 1.0.0
 */
public class BusinessException extends ErpException {

    /**
     * Creates a new business exception.
     *
     * @param errorCode the error code
     * @param message the error message
     */
    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a new business exception with details.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param details additional error details
     */
    public BusinessException(String errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    /**
     * Creates a new business exception with a cause.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param cause the root cause
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
