package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request does not carry a {@code X-Branch-Id} header
 * and the operation requires a branch context.
 *
 * @since 1.0.0
 */
public class MissingBranchContextException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public MissingBranchContextException() {
        super(HttpStatus.BAD_REQUEST, "MISSING_BRANCH_CONTEXT",
              "Branch context is missing; set X-Branch-Id header", null);
    }
}