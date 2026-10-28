package com.erp.platform.security.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for validating platform access tokens.
 *
 * <p>Bound from {@code erp.security.jwt.*}. Every service that consumes
 * access tokens must share the same secret and issuer as the Identity Service.
 *
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "erp.security.jwt")
public class PlatformJwtProperties {

    /**
     * HMAC secret shared with the Identity Service. Must be identical across services.
     * Must be at least 32 bytes (256 bits) for HS256. No insecure default is provided;
     * the value must be supplied via configuration / secrets manager.
     */
    @NotBlank(message = "JWT secret must be configured")
    @Size(min = 32, message = "JWT secret must be at least 32 bytes for HS256")
    private String secret;

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
