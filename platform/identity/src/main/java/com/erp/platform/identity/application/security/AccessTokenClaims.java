package com.erp.platform.identity.application.security;

import java.time.Instant;
import java.util.List;

/**
 * Parsed claims of a validated access token.
 *
 * @param userId    the authenticated user's UUID (subject)
 * @param tenantId  the tenant the user belongs to
 * @param username  the user's username
 * @param email     the user's email
 * @param fullName  the user's full name
 * @param roles     the user's active role codes
 * @param jti       the JWT identifier
 * @param issuedAt  issuance instant
 * @param expiresAt expiry instant
 *
 * @since 1.0.0
 */
public record AccessTokenClaims(
        String userId,
        Long tenantId,
        String username,
        String email,
        String fullName,
        List<String> roles,
        String jti,
        Instant issuedAt,
        Instant expiresAt) {
}
