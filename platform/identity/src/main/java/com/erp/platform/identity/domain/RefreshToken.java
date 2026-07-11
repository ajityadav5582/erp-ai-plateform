package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token aggregate for the authentication subsystem.
 *
 * <p>Refresh tokens are opaque, revocable credentials that allow a client to
 * obtain a new access token without re-entering credentials. They are stored
 * server-side (only a SHA-256 hash of the raw token is persisted) so they can be
 * individually revoked (e.g. on logout) and audited.
 *
 * <p>Lifecycle:
 * <pre>
 *     Issued (active) ──▶ Revoked (logout / password reset)
 *            │
 *            └──────────▶ Expired (after refresh-token-expiry-ms)
 * </pre>
 *
 * <p>Refresh token rotation is enforced by the application layer: every refresh
 * request revokes the presented token and issues a brand-new one.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_id", columnNames = "token_id"),
        indexes = {
            @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
            @Index(name = "idx_refresh_tokens_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash"),
            @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class RefreshToken {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Globally unique business identifier (JWT {@code jti}) for the token.
     */
    @Column(name = "token_id", nullable = false, updatable = false)
    @UuidGenerator
    private UUID tokenId;

    /**
     * Reference to the owning user (internal surrogate key).
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /**
     * Tenant the token belongs to (multi-tenant isolation).
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /**
     * SHA-256 hash of the raw opaque refresh token.
     * The raw token is only ever returned to the client, never persisted.
     */
    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    /**
     * Instant after which the token can no longer be used.
     */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /**
     * Instant the token was revoked (logout / password reset), or null if still active.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Principal (user id / system) that revoked the token, for audit purposes.
     */
    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    /**
     * Free-form device/browser description supplied at login (best-effort).
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * Originating IP address captured at login (best-effort, for audit).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Instant the token record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Factory method to issue a new refresh token.
     *
     * @param tokenId    the unique token id (jti)
     * @param userId     the owning user id
     * @param tenantId   the tenant id
     * @param tokenHash  SHA-256 hash of the raw token
     * @param expiresAt  expiry instant
     * @param deviceInfo optional device description
     * @param ipAddress  optional client IP
     * @param createdAt  creation instant
     * @return a new, active {@link RefreshToken}
     */
    public static RefreshToken issue(UUID tokenId, Long userId, Long tenantId,
                                     String tokenHash, Instant expiresAt,
                                     String deviceInfo, String ipAddress, Instant createdAt) {
        return RefreshToken.builder()
                .tokenId(tokenId)
                .userId(userId)
                .tenantId(tenantId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .createdAt(createdAt)
                .build();
    }

    /**
     * Revokes this token.
     *
     * @param revokedAt the revocation instant
     * @param revokedBy the principal performing the revocation
     */
    public void revoke(Instant revokedAt, String revokedBy) {
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
    }

    /**
     * @return true if the token has been explicitly revoked
     */
    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    /**
     * @return true if the token is past its expiry instant
     */
    public boolean isExpired(Instant now) {
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }

    /**
     * @return true if the token can still be used (not revoked and not expired)
     */
    public boolean isUsable(Instant now) {
        return !isRevoked() && !isExpired(now);
    }
}
