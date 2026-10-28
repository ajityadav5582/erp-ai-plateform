package com.erp.platform.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cryptographically validates platform access tokens issued by the Identity Service.
 *
 * <p>This is a pure, dependency-free validator (no database, no network). Downstream
 * business services use it to establish who the caller is and what they may do,
 * entirely from the token itself.
 *
 * @since 1.0.0
 */
public class PlatformTokenValidator {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String TOKEN_TYPE_ACCESS = "access";

    private final SecretKey key;
    private final String issuer;

    public PlatformTokenValidator(String secret, String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    /**
     * Parses and validates an access token.
     *
     * @param token the compact JWT string
     * @return the extracted claims
     * @throws JwtException if the token is malformed, has a bad signature, is expired,
     *                      has the wrong issuer, or is not an access token
     */
    public PlatformTokenClaims validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Token is not an access token");
        }

        Long tenantId = claims.get(CLAIM_TENANT_ID, Number.class) == null
                ? null
                : claims.get(CLAIM_TENANT_ID, Number.class).longValue();

        return new PlatformTokenClaims(
                claims.getSubject(),
                tenantId,
                claims.get("username", String.class),
                claims.get("email", String.class),
                claims.get(CLAIM_FULL_NAME, String.class),
                readStringList(claims, CLAIM_ROLES),
                readStringList(claims, CLAIM_PERMISSIONS),
                claims.getId(),
                claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant(),
                claims.getExpiration() == null ? null : claims.getExpiration().toInstant());
    }

    /**
     * @return true if the token is present and cryptographically valid.
     */
    public boolean isValid(String token) {
        try {
            validate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Claims claims, String name) {
        List<String> values = (List<String>) (List<?>) claims.get(name, List.class);
        return values == null ? List.of() : List.copyOf(values);
    }
}
