package com.erp.platform.identity.domain.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for role domain operations.
 *
 * <p>Extends {@link BaseApiException} so each failure declares its own HTTP status
 * and error code instead of requiring a dedicated {@code @ExceptionHandler} method.
 *
 * <p>The plain constructor defaults to {@code 409 ROLE_OPERATION_CONFLICT}.
 * Subclasses pass a more specific status and code when they need one.
 *
 * @since 1.0.0
 */
public class RoleOperationException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public RoleOperationException(String message) {
        this(HttpStatus.CONFLICT, "ROLE_OPERATION_CONFLICT", message);
    }

    protected RoleOperationException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }
}
