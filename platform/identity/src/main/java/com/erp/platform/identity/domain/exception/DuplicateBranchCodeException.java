package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a branch code already exists within a tenant/company scope.
 *
 * <p>Branch codes must be unique within a company scope. Maps to
 * HTTP 409 Conflict via {@link com.erp.platform.common.exception.BaseApiException}.
 *
 * @since 1.0.0
 */
public class DuplicateBranchCodeException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public DuplicateBranchCodeException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_BRANCH_CODE", message, null);
    }

    public DuplicateBranchCodeException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "DUPLICATE_BRANCH_CODE", message, cause);
    }
}
