package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request carries no company context.
 *
 * <p>The company (legal entity) is selected by the user after login and sent on
 * every subsequent request via the {@code X-Company-Id} header. Requests that
 * need company-scoped data but arrive without it are rejected rather than
 * silently falling back to "all companies".
 *
 * <p>Reports {@code 400 MISSING_COMPANY_CONTEXT}.
 *
 * @since 1.0.0
 */
public class MissingCompanyContextException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public MissingCompanyContextException() {
        super(HttpStatus.BAD_REQUEST, "MISSING_COMPANY_CONTEXT",
                "No company is selected for the current request. Select a company first.");
    }

    public MissingCompanyContextException(String message) {
        super(HttpStatus.BAD_REQUEST, "MISSING_COMPANY_CONTEXT", message);
    }
}
