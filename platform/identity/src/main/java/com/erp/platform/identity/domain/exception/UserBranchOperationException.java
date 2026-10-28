package com.erp.platform.identity.domain.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for user-branch domain operations.
 *
 * <p>This is the parent class for all domain-specific exceptions
 * related to user-branch assignment lifecycle and business rule violations.
 *
 * <p>Extends {@link BaseApiException} so each failure declares its own HTTP status
 * and error code instead of requiring a dedicated {@code @ExceptionHandler} method.
 * Previously this class extended {@code RuntimeException}, so expected failures
 * (for example a missing user-branch assignment) were reported as HTTP 500
 * {@code INTERNAL_ERROR} and logged as server faults.
 *
 * <p>The plain constructor defaults to {@code 404 USER_BRANCH_NOT_FOUND}, which is
 * the common case for this hierarchy. Subclasses pass a more specific status and
 * code when they need one.
 *
 * @since 1.0.0
 */
public abstract class UserBranchOperationException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a user-branch failure reported as HTTP 404 Not Found.
     *
     * @param message the human-readable message
     */
    protected UserBranchOperationException(String message) {
        this(HttpStatus.NOT_FOUND, "USER_BRANCH_NOT_FOUND", message, null);
    }

    /**
     * Creates a user-branch failure reported as HTTP 404 Not Found, with a cause.
     *
     * @param message the human-readable message
     * @param cause   the root cause
     */
    protected UserBranchOperationException(String message, Throwable cause) {
        this(HttpStatus.NOT_FOUND, "USER_BRANCH_NOT_FOUND", message, cause);
    }

    /**
     * Creates a user-branch failure with an explicit HTTP status and error code.
     *
     * @param status    the HTTP status to respond with
     * @param errorCode the machine-readable error code
     * @param message   the human-readable message
     * @param cause     the root cause, may be null
     */
    protected UserBranchOperationException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(status, errorCode, message, cause);
    }
}
