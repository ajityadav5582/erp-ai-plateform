package com.erp.platform.identity.domain.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for authentication failures.
 *
 * <p>Extends {@link BaseApiException} so each failure declares its own HTTP status
 * and error code instead of requiring a dedicated {@code @ExceptionHandler} method.
 *
 * <p>The plain constructor defaults to {@code 401 AUTHENTICATION_ERROR}, matching
 * the response this exception produced before. Subclasses that represent a
 * different outcome (locked account, invalid refresh token) pass their own.
 *
 * @since 1.0.0
 */
public class AuthenticationException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        this(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR", message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR", message, cause);
    }

    protected AuthenticationException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }
}
