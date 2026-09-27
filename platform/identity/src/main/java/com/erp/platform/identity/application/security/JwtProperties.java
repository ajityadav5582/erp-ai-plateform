package com.erp.platform.identity.application.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Configuration properties for JWT issuance and verification.
 *
 * <p>Bound from the {@code auth.jwt.*} namespace in {@code application.yml}.
 *
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /**
     * HMAC-SHA signing secret. Must be at least 32 bytes (256 bits) for HS256.
     * Injected from a secrets manager in production.
     */
    @NotBlank(message = "JWT secret must be configured")
    @Size(min = 32, message = "JWT secret must be at least 32 bytes for HS256")
    private String secret;

    /** JWT issuer claim value. */
    private String issuer = "erp-ai-platform-identity";

    /** Access token lifetime in milliseconds (default 15 minutes). */
    private long accessTokenExpiryMs = 900_000L;

    /** Refresh token lifetime in milliseconds (default 7 days). */
    private long refreshTokenExpiryMs = 604_800_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }

    public void setAccessTokenExpiryMs(long accessTokenExpiryMs) {
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }

    public void setRefreshTokenExpiryMs(long refreshTokenExpiryMs) {
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }
}
