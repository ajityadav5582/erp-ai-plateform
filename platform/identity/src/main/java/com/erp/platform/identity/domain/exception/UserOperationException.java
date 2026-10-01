package com.erp.platform.identity.domain.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for user domain operations.
 *
 * <p>Extends {@link BaseApiException} so each failure declares its own HTTP status
 * and error code. That removes the need for a dedicated
 * {@code @ExceptionHandler} method per subclass in the global exception handler.
 *
 * <p>The no-arg-status constructors default to {@code 409 USER_OPERATION_CONFLICT},
 * preserving the response that {@code UserOperationException} produced before.
 * Subclasses that mean something more specific (for example
 * {@link UserNotFoundException}) pass their own status and code.
 *
 * @since 1.0.0
 */
public abstract class UserOperationException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    protected UserOperationException(String message) {
        this(HttpStatus.CONFLICT, "USER_OPERATION_CONFLICT", message);
    }

    protected UserOperationException(String message, Throwable cause) {
        this(HttpStatus.CONFLICT, "USER_OPERATION_CONFLICT", message, cause);
    }

    protected UserOperationException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    protected UserOperationException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(status, errorCode, message, cause);
    }
}
