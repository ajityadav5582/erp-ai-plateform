package com.erp.platform.security.token;

import java.time.Instant;
import java.util.List;

/**
 * Claims carried by a platform access token.
 *
 * <p>Issued by the Identity Service and validated by every downstream service
 * (business modules such as inventory, and future platform services) without
 * any cross-service call.
 *
 * <p>Note that the active <b>company</b> and <b>fiscal year</b> are intentionally
 * NOT part of these claims. They are selected by the user after login and
 * carried per-request via the {@code X-Company-Id} / {@code X-Fiscal-Year-Id}
 * headers, so the user can switch context without re-authenticating.
 *
 * @param userId      the authenticated user's numeric id (token subject)
 * @param tenantId    the tenant the user belongs to
 * @param username    the user's username
 * @param email       the user's email
 * @param fullName    the user's full name
 * @param roles       the user's active role codes
 * @param permissions the permission codes granted to the user's roles
 * @param jti         the token identifier
 * @param issuedAt    issuance instant
 * @param expiresAt   expiry instant
 *
 * @since 1.0.0
 */
public record PlatformTokenClaims(
        String userId,
        Long tenantId,
        String username,
        String email,
        String fullName,
        List<String> roles,
        List<String> permissions,
        String jti,
        Instant issuedAt,
        Instant expiresAt) {
}
