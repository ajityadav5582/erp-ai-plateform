package com.erp.platform.identity.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response returned after a successful authentication operation (login or token refresh).
 *
 * <p>Contains the short-lived access token (JWT) and the opaque refresh token. The
 * raw refresh token is returned exactly once and must be stored securely by the client.
 *
 * @param accessToken            the signed JWT access token (Bearer)
 * @param refreshToken           the opaque refresh token (raw value, returned only once)
 * @param tokenType              the token type, always {@code "Bearer"}
 * @param accessTokenExpiresIn   access token lifetime in seconds
 * @param refreshTokenExpiresIn  refresh token lifetime in seconds
 * @param userId                 the authenticated user's business identifier (UUID)
 * @param tenantId               the tenant the user belongs to
 * @param username               the user's username
 * @param email                  the user's email
 * @param fullName               the user's full name
 * @param roles                  the user's active role codes
 *
 * @since 1.0.0
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        UUID userId,
        Long tenantId,
        String username,
        String email,
        String fullName,
        List<String> roles) {

    /**
     * Convenience factory for a Bearer token response.
     */
    public static TokenResponse bearer(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInSeconds,
            long refreshTokenExpiresInSeconds,
            UUID userId,
            Long tenantId,
            String username,
            String email,
            String fullName,
            List<String> roles) {
        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiresInSeconds,
                refreshTokenExpiresInSeconds,
                userId,
                tenantId,
                username,
                email,
                fullName,
                roles);
    }
}
