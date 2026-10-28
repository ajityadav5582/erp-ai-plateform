package com.erp.platform.identity.application.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for issuing and verifying JSON Web Tokens.
 *
 * <p>Two token types are produced:
 * <ul>
 *   <li><b>Access tokens</b> &ndash; short-lived JWTs (HS256) carrying the principal's
 *       identity and roles, validated on every protected request by the
 *       {@link com.erp.platform.identity.interfaces.rest.filter.JwtAuthenticationFilter}.</li>
 *   <li><b>Refresh tokens</b> &ndash; opaque, cryptographically-random strings (not JWTs)
 *       whose SHA-256 hash is stored server-side so they can be individually revoked.</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Service
public class JwtService {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final JwtProperties properties;
    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a signed access token (JWT) for the given principal.
     *
     * <p>The token carries the user's roles <em>and</em> the resolved permission codes.
     * Embedding permissions lets downstream business services authorize a request purely
     * from the validated token, with no cross-service call and no per-request database
     * hit. The trade-off is that a permission change only takes effect when the next
     * token is issued, which is why the access-token TTL should stay short (15 minutes).
     *
     * @param userId      the user's id (token subject)
     * @param tenantId    the tenant the user belongs to
     * @param username    the user's username
     * @param email       the user's email
     * @param fullName    the user's full name
     * @param roles       the user's active role codes
     * @param permissions the permission codes granted by those roles
     * @return the compact JWT string
     */
    public String generateAccessToken(String userId, Long tenantId, String username,
                                      String email, String fullName, Collection<String> roles,
                                      Collection<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getAccessTokenExpiryMs());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_TENANT_ID, tenantId)
                .claim("username", username)
                .claim("email", email)
                .claim(CLAIM_FULL_NAME, fullName)
                .claim(CLAIM_ROLES, roles == null ? List.of() : List.copyOf(roles))
                .claim(CLAIM_PERMISSIONS, permissions == null ? List.of() : List.copyOf(permissions))
                .signWith(key)
                .compact();
    }

    /**
     * Parses and cryptographically validates an access token.
     *
     * @param token the compact JWT string
     * @return the extracted {@link AccessTokenClaims}
     * @throws JwtException if the token is malformed, has a bad signature, is expired,
     *                      or is not an access token
     */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Token is not an access token");
        }

        Long tenantId = claims.get(CLAIM_TENANT_ID, Long.class);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) (List<?>) claims.get(CLAIM_ROLES, List.class);
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) (List<?>) claims.get(CLAIM_PERMISSIONS, List.class);
        return new AccessTokenClaims(
                claims.getSubject(),
                tenantId,
                claims.get("username", String.class),
                claims.get("email", String.class),
                claims.get(CLAIM_FULL_NAME, String.class),
                roles == null ? List.of() : roles,
                permissions == null ? List.of() : permissions,
                claims.getId(),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant());
    }

    /**
     * @return true if the access token is present and cryptographically valid (signature + expiry).
     */
    public boolean isAccessTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Generates a new opaque refresh token value (raw, unhashed).
     *
     * <p>The caller is responsible for hashing this value (SHA-256) before persistence
     * and returning the raw value to the client exactly once.
     *
     * @return a cryptographically-random refresh token string
     */
    public String generateRawRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * @return a new unique identifier for a refresh token (used as the JWT {@code jti}
     *         and stored as {@code token_id}).
     */
    public String generateRefreshTokenId() {
        return UUID.randomUUID().toString();
    }
}
