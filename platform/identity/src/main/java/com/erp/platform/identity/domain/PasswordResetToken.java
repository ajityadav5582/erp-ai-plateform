package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Password reset token aggregate for the authentication subsystem.
 *
 * <p>Represents a single-use, time-bound token that authorises a password change.
 * Only a SHA-256 hash of the raw token is persisted; the raw token is delivered
 * to the user out-of-band (e.g. email) and presented back on the reset call.
 *
 * <p>Lifecycle:
 * <pre>
 *     Issued (active) ──▶ Used (consumed by a successful reset)
 *            │
 *            └──────────▶ Expired (after password-reset.token-expiry-ms)
 * </pre>
 *
 * <p>For security, at most one active token should exist per user at a time;
 * the application layer invalidates any previously active tokens when a new one
 * is issued.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "password_reset_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_tokens_token_id", columnNames = "token_id"),
        indexes = {
            @Index(name = "idx_password_reset_tokens_user_id", columnList = "user_id"),
            @Index(name = "idx_password_reset_tokens_tenant_id", columnList = "tenant_id"),
            @Index(name = "idx_password_reset_tokens_token_hash", columnList = "token_hash"),
            @Index(name = "idx_password_reset_tokens_expires_at", columnList = "expires_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class PasswordResetToken {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Globally unique business identifier for the token.
     */
    @Column(name = "token_id", nullable = false, updatable = false)
    @UuidGenerator
    private UUID tokenId;

    /**
     * Reference to the user requesting the reset.
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /**
     * Tenant the token belongs to (multi-tenant isolation).
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /**
     * SHA-256 hash of the raw reset token.
     */
    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    /**
     * Instant after which the token can no longer be used.
     */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /**
     * Instant the token was consumed by a successful password reset, or null.
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Instant the token record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Factory method to issue a new password reset token.
     *
     * @param tokenId   the unique token id
     * @param userId    the requesting user id
     * @param tenantId  the tenant id
     * @param tokenHash SHA-256 hash of the raw token
     * @param expiresAt expiry instant
     * @param createdAt creation instant
     * @return a new, active {@link PasswordResetToken}
     */
    public static PasswordResetToken issue(UUID tokenId, Long userId, Long tenantId,
                                           String tokenHash, Instant expiresAt, Instant createdAt) {
        return PasswordResetToken.builder()
                .tokenId(tokenId)
                .userId(userId)
                .tenantId(tenantId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();
    }

    /**
     * Marks the token as consumed by a successful password reset.
     *
     * @param usedAt the instant the token was used
     */
    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt;
    }

    /**
     * @return true if the token has already been used
     */
    public boolean isUsed() {
        return this.usedAt != null;
    }

    /**
     * @return true if the token is past its expiry instant
     */
    public boolean isExpired(Instant now) {
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }

    /**
     * @return true if the token can still be used (not used and not expired)
     */
    public boolean isUsable(Instant now) {
        return !isUsed() && !isExpired(now);
    }
}
