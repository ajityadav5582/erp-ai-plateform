package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a branch cannot be deleted.
 *
 * <p>Raised when the branch still has dependent records (for example departments)
 * that would be blocked by the {@code ON DELETE RESTRICT} foreign key. Maps to
 * HTTP 409 Conflict via {@link com.erp.platform.common.exception.BaseApiException}.
 *
 * @since 1.0.0
 */
public class CannotDeleteBranchException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public CannotDeleteBranchException(String message) {
        super(HttpStatus.CONFLICT, "CANNOT_DELETE_BRANCH", message, null);
    }

    public CannotDeleteBranchException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "CANNOT_DELETE_BRANCH", message, cause);
    }
}
