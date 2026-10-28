package com.erp.platform.identity.domain.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for branch domain operations.
 *
 * <p>This is the parent class for all domain-specific exceptions
 * related to branch lifecycle and business rule violations.
 *
 * <p>Extends {@link BaseApiException} so each failure declares its own HTTP status
 * and error code instead of requiring a dedicated {@code @ExceptionHandler} method
 * in every service. Previously this class extended {@code RuntimeException}, so
 * expected branch failures (duplicate code/name, missing branch, blocked delete)
 * were reported as HTTP 500 {@code INTERNAL_ERROR} and logged as server faults.
 *
 * <p>The plain constructor defaults to {@code 409 BRANCH_OPERATION_CONFLICT}.
 * Subclasses pass a more specific status and code when they need one:
 * <ul>
 *   <li>{@link BranchNotFoundException} &rarr; {@code 404 BRANCH_NOT_FOUND}</li>
 *   <li>{@link DuplicateBranchCodeException} &rarr; {@code 409 DUPLICATE_BRANCH_CODE}</li>
 *   <li>{@link DuplicateBranchNameException} &rarr; {@code 409 DUPLICATE_BRANCH_NAME}</li>
 *   <li>{@link CannotDeleteBranchException} &rarr; {@code 409 CANNOT_DELETE_BRANCH}</li>
 * </ul>
 *
 * @since 1.0.0
 */
public abstract class BranchOperationException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a branch failure reported as HTTP 409 Conflict.
     *
     * @param message the human-readable message
     */
    protected BranchOperationException(String message) {
        this(HttpStatus.CONFLICT, "BRANCH_OPERATION_CONFLICT", message, null);
    }

    /**
     * Creates a branch failure reported as HTTP 409 Conflict, with a cause.
     *
     * @param message the human-readable message
     * @param cause   the root cause
     */
    protected BranchOperationException(String message, Throwable cause) {
        this(HttpStatus.CONFLICT, "BRANCH_OPERATION_CONFLICT", message, cause);
    }

    /**
     * Creates a branch failure with an explicit HTTP status and error code.
     *
     * @param status    the HTTP status to respond with
     * @param errorCode the machine-readable error code
     * @param message   the human-readable message
     * @param cause     the root cause, may be null
     */
    protected BranchOperationException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(status, errorCode, message, cause);
    }
}
