package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserRole association entity.
 *
 * <p>Provides methods for managing the many-to-many relationship between
 * User and Role with additional assignment metadata.
 *
 * @since 1.0.0
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    Optional<UserRole> findByUserRoleId(UUID userRoleId);

    /**
     * Finds all active role assignments for a user.
     *
     * <p>Active means: not revoked and not expired.
     *
     * @param userId the user ID
     * @param now the current timestamp for expiration check
     * @return list of active user role assignments
     */
    @Query("""
            SELECT ur FROM UserRole ur
            WHERE ur.userId = :userId
              AND ur.revokedAt IS NULL
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    List<UserRole> findActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Finds all active role assignments for a user in a specific tenant.
     *
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param now the current timestamp for expiration check
     * @return list of active user role assignments
     */
    @Query("""
            SELECT ur FROM UserRole ur
            WHERE ur.userId = :userId
              AND ur.tenantId = :tenantId
              AND ur.revokedAt IS NULL
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    List<UserRole> findActiveByUserIdAndTenantId(
            @Param("userId") Long userId,
            @Param("tenantId") Long tenantId,
            @Param("now") Instant now);

    /**
     * Finds the primary role assignment for a user.
     *
     * @param userId the user ID
     * @param now the current timestamp for expiration check
     * @return the primary user role assignment, if any
     */
    @Query("""
            SELECT ur FROM UserRole ur
            WHERE ur.userId = :userId
              AND ur.isPrimaryRole = true
              AND ur.revokedAt IS NULL
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    Optional<UserRole> findPrimaryByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Finds all users assigned to a specific role.
     *
     * @param roleId the role ID
     * @param now the current timestamp for expiration check
     * @return list of user role assignments for the role
     */
    @Query("""
            SELECT ur FROM UserRole ur
            WHERE ur.roleId = :roleId
              AND ur.revokedAt IS NULL
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    List<UserRole> findActiveByRoleId(@Param("roleId") Long roleId, @Param("now") Instant now);

    /**
     * Finds all users assigned to a specific role in a tenant.
     *
     * @param roleId the role ID
     * @param tenantId the tenant ID
     * @param now the current timestamp for expiration check
     * @return list of user role assignments for the role
     */
    @Query("""
            SELECT ur FROM UserRole ur
            WHERE ur.roleId = :roleId
              AND ur.tenantId = :tenantId
              AND ur.revokedAt IS NULL
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    List<UserRole> findActiveByRoleIdAndTenantId(
            @Param("roleId") Long roleId,
            @Param("tenantId") Long tenantId,
            @Param("now") Instant now);

    /**
     * Checks if a user has a specific role assigned and active.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @param now the current timestamp for expiration check
     * @return true if the user has the role assigned and active, false otherwise
     */
    @Query("""
            SELECT COUNT(ur) > 0 FROM UserRole ur
            WHERE ur.userId = :userId
              AND ur.roleId = :roleId
              AND ur.revokedAt IS NULL
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    boolean existsActiveByUserIdAndRoleId(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId,
            @Param("now") Instant now);

    /**
     * Finds all assignments for a user (including expired and revoked).
     *
     * @param userId the user ID
     * @return list of all user role assignments
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * Finds all assignments for a role (including expired and revoked).
     *
     * @param roleId the role ID
     * @return list of all user role assignments
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * Finds all assignments in a tenant.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination information
     * @return page of user role assignments
     */
    Page<UserRole> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Finds all active assignments in a tenant.
     *
     * @param tenantId the tenant ID
     * @param now the current timestamp for expiration check
     * @param pageable pagination information
     * @return page of active user role assignments
     */
    @Query("""
            SELECT ur FROM UserRole ur
            WHERE ur.tenantId = :tenantId
              AND ur.revokedAt IS NULL
              AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)
            """)
    Page<UserRole> findActiveByTenantId(
            @Param("tenantId") Long tenantId,
            @Param("now") Instant now,
            Pageable pageable);

    /**
     * Finds all expired assignments.
     *
     * @param now the current timestamp
     * @return list of expired user role assignments
     */
    @Query("""
            SELECT ur FROM UserRole ur
            WHERE ur.expiresAt IS NOT NULL
              AND ur.expiresAt <= :now
              AND ur.revokedAt IS NULL
            """)
    List<UserRole> findExpired(@Param("now") Instant now);

    /**
     * Deletes all assignments for a user.
     *
     * @param userId the user ID
     * @return number of deleted assignments
     */
    long deleteByUserId(Long userId);

    /**
     * Deletes all assignments for a role.
     *
     * @param roleId the role ID
     * @return number of deleted assignments
     */
    long deleteByRoleId(Long roleId);

    boolean existsByUserRoleId(UUID userRoleId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Finds a specific user role assignment by user ID and role ID.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @return the user role assignment, if any
     */
    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Finds all assignments for a user with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of user role assignments
     */
    Page<UserRole> findByUserId(Long userId, Pageable pageable);

    /**
     * Finds all assignments for a role with pagination.
     *
     * @param roleId the role ID
     * @param pageable pagination information
     * @return page of user role assignments
     */
    Page<UserRole> findByRoleId(Long roleId, Pageable pageable);
}
