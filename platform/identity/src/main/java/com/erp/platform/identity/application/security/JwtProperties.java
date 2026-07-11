package com.erp.platform.identity.application.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT issuance and verification.
 *
 * <p>Bound from the {@code auth.jwt.*} namespace in {@code application.yml}.
 *
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /**
     * HMAC-SHA signing secret. Must be at least 32 bytes (256 bits) for HS256.
     * Injected from a secrets manager in production.
     */
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
