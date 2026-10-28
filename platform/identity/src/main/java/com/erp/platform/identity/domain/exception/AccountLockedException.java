package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Exception thrown when an account is locked.
 *
 * <p>Reports {@code 403 ACCOUNT_LOCKED}.
 *
 * @since 1.0.0
 */
public class AccountLockedException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    private final Instant retryAfter;

    public AccountLockedException(String message) {
        this(message, null);
    }

    public AccountLockedException(String message, Instant retryAfter) {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", message);
        this.retryAfter = retryAfter;
    }

    /**
     * @return the instant at which the temporary lock expires, or null for a
     *         permanent (administrative) lock
     */
    public Instant getRetryAfter() {
        return retryAfter;
    }
}
