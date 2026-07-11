package com.erp.platform.identity.domain.exception;

import java.time.Instant;

/**
 * Thrown when an account cannot be authenticated because it is locked.
 *
 * <p>This covers both the administrative {@link com.erp.platform.identity.domain.UserStatus#LOCKED}
 * lifecycle state and a temporary lockout caused by too many failed login attempts.
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
        super(message);
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
