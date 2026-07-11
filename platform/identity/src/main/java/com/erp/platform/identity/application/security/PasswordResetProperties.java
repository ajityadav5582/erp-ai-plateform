package com.erp.platform.identity.application.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the password-reset subsystem.
 *
 * <p>Bound from the {@code auth.password-reset.*} namespace in {@code application.yml}.
 *
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "auth.password-reset")
public class PasswordResetProperties {

    /** Reset token lifetime in milliseconds (default 1 hour). */
    private long tokenExpiryMs = 3_600_000L;

    /** Maximum consecutive failed login attempts before temporary lockout. */
    private int maxFailedLoginAttempts = 5;

    /** Temporary lockout duration after exceeding max attempts (milliseconds). Default 15 minutes. */
    private long lockoutDurationMs = 900_000L;

    /**
     * When {@code true} the raw reset token is returned in the forgot-password response.
     * This is for development/testing only. In production this MUST be {@code false} and
     * the token must be delivered out-of-band (e.g. email).
     */
    private boolean returnTokenInResponse = false;

    public long getTokenExpiryMs() {
        return tokenExpiryMs;
    }

    public void setTokenExpiryMs(long tokenExpiryMs) {
        this.tokenExpiryMs = tokenExpiryMs;
    }

    public int getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }

    public void setMaxFailedLoginAttempts(int maxFailedLoginAttempts) {
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
    }

    public long getLockoutDurationMs() {
        return lockoutDurationMs;
    }

    public void setLockoutDurationMs(long lockoutDurationMs) {
        this.lockoutDurationMs = lockoutDurationMs;
    }

    public boolean isReturnTokenInResponse() {
        return returnTokenInResponse;
    }

    public void setReturnTokenInResponse(boolean returnTokenInResponse) {
        this.returnTokenInResponse = returnTokenInResponse;
    }
}
