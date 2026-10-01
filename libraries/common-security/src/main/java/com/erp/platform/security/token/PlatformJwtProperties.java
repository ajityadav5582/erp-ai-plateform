package com.erp.platform.security.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for validating platform access tokens.
 *
 * <p>Bound from {@code erp.security.jwt.*}. Every service that consumes
 * access tokens must share the same secret and issuer as the Identity Service.
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "erp.security.jwt")
public class PlatformJwtProperties {

    /**
     * HMAC secret shared with the Identity Service. Must be identical across services.
     */
    private String secret = "jwt-secret-key-change-me-in-production-32b";

    /**
     * Expected token issuer. Must match the Identity Service issuer.
     */
    private String issuer = "erp-ai-platform-identity";

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
}
