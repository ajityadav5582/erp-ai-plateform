package com.erp.platform.common.exception;

/**
 * Exception for general business errors.
 *
 * @since 1.0.0
 */
public class ErpBusinessException extends BusinessException {

    /**
     * Creates a new business exception.
     *
     * @param message the error message
     */
    public ErpBusinessException(String message) {
        super("BUSINESS_ERROR", message);
    }

    /**
     * Creates a new business exception with an error code.
     *
     * @param errorCode the error code
     * @param message the error message
     */
    public ErpBusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a new business exception with details.
     *
     * @param message the error message
     * @param details additional error details
     */
    public ErpBusinessException(String message, java.util.Map<String, Object> details) {
        super("BUSINESS_ERROR", message, details);
    }
}
