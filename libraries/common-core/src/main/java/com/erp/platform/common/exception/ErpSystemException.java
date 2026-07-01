package com.erp.platform.common.exception;

/**
 * Exception for unexpected system errors.
 *
 * <p>This is a runtime exception that wraps unexpected errors.
 * It should be caught by the global exception handler and logged.
 *
 * @since 1.0.0
 */
public class ErpSystemException extends ErpException {

    /**
     * Creates a new system exception.
     *
     * @param message the error message
     * @param cause the root cause
     */
    public ErpSystemException(String message, Throwable cause) {
        super("INTERNAL_ERROR", message, cause);
    }

    /**
     * Creates a new system exception.
     *
     * @param message the error message
     */
    public ErpSystemException(String message) {
        super("INTERNAL_ERROR", message);
    }
}
