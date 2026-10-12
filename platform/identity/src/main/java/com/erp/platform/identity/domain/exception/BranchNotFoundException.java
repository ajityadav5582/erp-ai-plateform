package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a branch cannot be found.
 *
 * <p>Maps to HTTP 404 Not Found via {@link com.erp.platform.common.exception.BaseApiException}.
 *
 * @since 1.0.0
 */
public class BranchNotFoundException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public BranchNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "BRANCH_NOT_FOUND", message, null);
    }

    public BranchNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "BRANCH_NOT_FOUND", message, cause);
    }
}
