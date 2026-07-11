package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for {@link PasswordResetToken} persistence.
 *
 * <p>Reset tokens are looked up by their SHA-256 hash. Issuing a new token
 * invalidates any previously active tokens for the same user.
 *
 * @since 1.0.0
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Marks all currently-active (unused, unexpired) reset tokens for a user as used,
     * so that only the most recently issued token remains valid.
     *
     * @return the number of tokens invalidated
     */
    @Modifying(clearAutomatically = true)
    @Query("""
           UPDATE PasswordResetToken t
           SET t.usedAt = :usedAt
           WHERE t.userId = :userId
             AND t.tenantId = :tenantId
             AND t.usedAt IS NULL
             AND t.expiresAt > :now
           """)
    int invalidateActiveByUserIdAndTenantId(
            @Param("userId") Long userId,
            @Param("tenantId") Long tenantId,
            @Param("usedAt") Instant usedAt,
            @Param("now") Instant now);

    /**
     * Deletes reset tokens that expired before the given instant (scheduled cleanup).
     *
     * @return the number of tokens deleted
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :before")
    int deleteExpired(@Param("before") Instant before);
}
