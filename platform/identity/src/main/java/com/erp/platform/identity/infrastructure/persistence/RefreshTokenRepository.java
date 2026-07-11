package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link RefreshToken} persistence and revocation.
 *
 * <p>Refresh tokens are looked up by their SHA-256 hash (never the raw value)
 * and can be revoked individually or all-at-once for a user (logout / logout-all).
 *
 * @since 1.0.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(UUID tokenId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndTenantId(Long userId, Long tenantId);

    /**
     * Revokes all currently-active refresh tokens for a user/tenant (used by logout-all
     * and after a password reset to force re-authentication).
     *
     * @return the number of tokens revoked
     */
    @Modifying(clearAutomatically = true)
    @Query("""
           UPDATE RefreshToken rt
           SET rt.revokedAt = :revokedAt, rt.revokedBy = :revokedBy
           WHERE rt.userId = :userId
             AND rt.tenantId = :tenantId
             AND rt.revokedAt IS NULL
             AND (rt.expiresAt IS NULL OR rt.expiresAt > :now)
           """)
    int revokeAllActiveByUserIdAndTenantId(
            @Param("userId") Long userId,
            @Param("tenantId") Long tenantId,
            @Param("revokedAt") Instant revokedAt,
            @Param("revokedBy") String revokedBy,
            @Param("now") Instant now);

    /**
     * Deletes refresh tokens that expired before the given instant (scheduled cleanup).
     *
     * @return the number of tokens deleted
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :before")
    int deleteExpired(@Param("before") Instant before);
}
