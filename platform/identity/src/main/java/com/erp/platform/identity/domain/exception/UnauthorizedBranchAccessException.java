package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the user is not authorized to access a given branch.
 *
 * <p>Returned as {@code 403 BRANCH_ACCESS_DENIED} when the authenticated user
 * has no branch assignment and no bypass permission for the resolved branch.
 *
 * @since 1.0.0
 */
public class UnauthorizedBranchAccessException extends BranchOperationException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedBranchAccessException(String message) {
        super(HttpStatus.FORBIDDEN, "BRANCH_ACCESS_DENIED", message, null);
    }
}